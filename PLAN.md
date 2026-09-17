# Plano: histórico local, push de mensagens/status e badge de não lidas

**Resumo:** o app passa a ser *offline-first*, com **Room como fonte de verdade da UI**. A rede só
alimenta o banco, trazendo apenas mensagens novas. Push via **FCM**, disparado por **webhooks do
GLPI** que chegam no backbone. O backbone fica **dono do estado lido/não lido** e o app espelha essa
contagem no badge. O backend é implementado por outra sessão: aqui ficam o **contrato** e a
implementação do app. O app é tolerante a um backend que ainda não tenha as rotas novas.

Decisões confirmadas: FCM · túnel em dev para o webhook · backend por outra sessão · badge = nº de
mensagens novas · lido sincronizado no backend · push de mensagens **e** mudança de status ·
permissão de notificação pedida logo após abrir o primeiro chamado.

---

## Contexto

### App (`kotlin/filament`) hoje
- A **rede é a fonte de verdade** e o estado vive só em memória. `HomeViewModel.loadTickets()` e
  `ChatViewModel.load()` buscam tudo a cada abertura, retomada ou pull-to-refresh. Só o bearer token é
  persistido (`data/auth/AuthTokenStore.kt`).
- A conversa vem de `domain/usecase/LoadConversationUseCase.kt`: abertura (id 0, montada a partir de
  `Ticket.description`) + `GET followups` inteiro.
- O envio acrescenta o eco do `POST` na lista em memória. Já houve uma corrida de id duplicado aqui,
  contornada em `ChatViewModel`.
- **Badge atual** = `HomeUiState.ticketsBadgeCount` = `tickets.size`, sem relação com mensagens.
- Sessão: `AuthorizedApiCall` limpa o token em 401; `MainViewModel.isSignedIn` troca a raiz da
  navegação; o deep link `filament://auth-callback` é tratado em `MainActivity` → `MainViewModel`.
- Stack: Hilt + KSP, Navigation 3 (`ChatRoute(ticketId, ticketTitle)`), minSdk 33, `applicationId
  com.raave.filament`. Sem Room, Firebase ou WorkManager. O emulador tem Google Play Services (FCM ok).

### Backend ativo: `~/repos/backbone` no WSL
O `Repositorios/backbone` no Windows está desatualizado.
- Hono + Better Auth + Prisma 7/Postgres, deploy alvo Vercel (serverless).
- GLPI 11 via **API legada** `apirest.php`, com conta de serviço única. Cada rota resolve o requerente
  por e-mail e checa a posse do chamado.
- `GET .../followups` devolve sempre a lista inteira. Não há tabela de dispositivos, push nem webhook.

### GLPI 11: webhooks nativos (conferido em `src/Webhook.php`, branch 11.0)
- Configurados por itemtype + evento (`new`/`update`/`delete`), com escopo por entidade.
- Assinatura: `X-GLPI-signature` = HMAC-SHA256(corpo + timestamp, segredo), mais `X-GLPI-timestamp`.
- Entrega **assíncrona pela fila `QueuedWebhook`, processada pelo cron do GLPI**. A latência do push
  depende da frequência desse cron.
- **A validar na instância:** se `ITILFollowup` aparece como itemtype suportado, e se adicionar um
  followup também dispara `Ticket`/`update`.

---

## Impacto no sistema

| | Antes | Depois |
|---|---|---|
| Fonte de verdade da UI | resposta HTTP em memória | **Room** (flows observados) |
| Histórico de mensagens | rebaixado inteiro a cada abertura | persistido; rede traz só `after=<último id>` |
| Não lidas | inexistente (badge = total de chamados) | **backbone** (contagem por usuário/chamado), espelhada no Room |
| Mensagem nova chega por | pull/abertura de tela | **push FCM** (grava direto no Room) + sync de fallback |
| Status do chamado | só ao recarregar a lista | push `ticket.status_changed` + lista |

**Novos invariantes**
- A UI **nunca** lê a rede diretamente: repositórios gravam no Room e ViewModels observam o Room.
- Upsert por id de mensagem. Isso torna push + sync + eco de envio idempotentes e elimina a classe de
  bug "id duplicado na LazyColumn". A proteção manual em `ChatViewModel` sai.
- Sair da conta ou receber 401 → **banco limpo**, notificações canceladas e token FCM apagado. Um push
  recebido deslogado é ignorado.
- Marcação de lido local é otimista: o chamado aparece com 0 na hora e o marcador vai para uma
  **fila pendente** (tabela) até o backend confirmar. Assim uma falha de rede não faz a contagem
  "voltar".
- Notificação só para mensagem **de outra pessoa**, fora da conversa aberta na tela. Mensagem própria
  (escrita em outro aparelho) chega por push silencioso só para sincronizar.

**Simplificações que isso permite**
- `ChatViewModel` perde a lista mutável, o eco manual e a deduplicação.
- A atualização ao retomar e o pull-to-refresh passam a ser "sync", não "recarregar tela".
- O badge sai de `HomeUiState` como estado derivado da soma de não lidas no Room, sem contador
  paralelo.

**Tolerância a backend parcial.** O app pode entrar antes do backbone:
- `after` ignorado pelo backend → o app filtra ids localmente.
- `unreadCount` ausente → 0, sem badge.
- `/api/devices` ou `/read` com 404 → falha silenciosa, com nova tentativa no próximo sync.

---

## Contrato com o backbone

> **Status (2026-09-17):** itens 1–5 implementados pela sessão do backbone (commit `c45510f`), sem
> mudança de nomes ou formatos. Falta validar o push real no FCM e os pontos do item 8 na instância.
> Detalhes que o backend acrescentou:
> - O GLPI valida a URL do webhook com um desafio: `GET ?crc_token=` → HMAC do token em texto puro.
> - `GlpiTicketState.lastFollowupId` é **marca d'água** (só avança), porque a fila do GLPI entrega fora
>   de ordem.
>
> - Túnel estável: `https://dev.elinsadobrasil.com.br` (Cloudflare named tunnel); webhook em
>   `/api/webhooks/glpi`.
> - **Item 8 validado na instância:**
>   - `ITILFollowup`/`new` dispara webhook, e adicionar um followup dispara também `Ticket`/`update`.
>     A dedupe por status evita push duplicado.
>   - A fila drena sem visita à UI (cron com intervalo curto). **Latência medida: ~30–41 s** entre
>     criar o followup e a entrega.
> - Armadilha registrada pelo backend: no corpo do webhook, `item.itemtype` é o tipo do *pai* do
>   followup ("Ticket"), não do item entregue. O discriminador confiável é a presença de `parent_item`.
>   Não afeta o app: o app só lê os payloads FCM montados pelo backend.
>
> **Consequência para o app:** push fora de ordem não pode regredir estado. `unreadCount` e status de
> um push só são aplicados se o evento for mais novo que o último aplicado para aquele chamado
> (`followupId` / `dateMod`). A mensagem em si é upsert por id, então a ordem de chegada não importa.


Todas as rotas `/api/*` exigem sessão Better Auth (bearer), exceto o webhook.

1. **`GET /api/glpi/tickets/:id/followups?after=<followupId>`**: só followups públicos com
   `id > after`, no mesmo formato de hoje. Sem `after`, lista inteira (compatível com o atual).
2. **`GET /api/glpi/tickets`**: cada item ganha `unreadCount: number` e
   `lastReadFollowupId: number | null`.
3. **`POST /api/glpi/tickets/:id/read`** `{ "lastReadFollowupId": number }` → `200 { unreadCount,
   lastReadFollowupId }`.
   - Marcador = max(atual, recebido).
   - Apaga as não lidas com `followupId <= marcador`.
   - Envia push `ticket.read` para os **outros** aparelhos do usuário.
4. **`POST /api/devices`** `{ "token": string, "platform": "android" }` → `204`.
   - Upsert por token, vinculado ao usuário e à **sessão atual** (FK `onDelete: Cascade`: sair apaga o
     vínculo sozinho).
   - Um token por sessão.
   - Token recusado pelo FCM (`registration-token-not-registered`) → apagar.
5. **`POST /api/webhooks/glpi`** (sem Better Auth):
   - **Validação:** assinatura HMAC-SHA256 (`GLPI_WEBHOOK_SECRET`) em comparação de tempo constante, e
     timestamp dentro de ±5 min.
   - **Reprocessamento:** o corpo serve **só como gatilho**. O backend relê o item pela API legada, que
     já tem os filtros e as formas conhecidas (`is_private`, autor, requerentes).
   - `ITILFollowup` / `new` → para cada requerente do chamado com conta no app:
     - se **não** é autor: grava a não lida (`followupId > marcador`, idempotente) e envia `followup.created`;
     - se é autor: só `followup.created` com `isMine=true`.
   - `Ticket` / `update` → compara com o status guardado. Se mudou, grava e envia
     `ticket.status_changed`. Na primeira vez que vê um chamado, só grava.
6. **Tabelas sugeridas:**
   - `DeviceToken(token PK, userId, sessionId FK cascade, platform, updatedAt)`
   - `TicketReadMarker(userId, glpiTicketId, lastReadFollowupId)` com PK composta
   - `UnreadFollowup(userId, glpiTicketId, followupId, createdAt)`, `@@id([userId, followupId])`
   - `GlpiTicketState(glpiTicketId PK, statusId, lastFollowupId)`, para detectar status e deduplicar
     entregas repetidas
7. **Payloads FCM**: *data-only*, prioridade alta, todos os valores string.
   - `type=followup.created`: `ticketId`, `ticketTitle`, `followupId`, `content` (mesmo HTML cru
     do endpoint), `contentTruncated`, `sentAt` (ISO com offset), `authorName`, `isMine`,
     `unreadCount`. Truncar `content` para caber no limite de 4 KB.
   - `type=ticket.status_changed`: `ticketId`, `ticketTitle`, `statusId`, `statusName`, `dateMod`.
   - `type=ticket.read`: `ticketId`, `lastReadFollowupId`, `unreadCount`.
8. **Dev/GLPI:**
   - Túnel com URL **estável**: domínio fixo do ngrok ou túnel nomeado do Cloudflare. A URL rápida do
     `cloudflared` muda a cada execução.
   - Em *Configurar > Webhooks*, dois webhooks: `ITILFollowup`/`new` e `Ticket`/`update`, com o mesmo
     segredo, na entidade "Elinsa do Brasil".
   - Confirmar que a ação automática `queuedwebhook` roda no cron em modo CLI.
   - Validar antes os dois pontos em aberto da seção GLPI. Se `ITILFollowup` não for suportado, detectar
     followups novos no `Ticket`/`update` via `GlpiTicketState.lastFollowupId`.
   - Credenciais: `firebase-admin` com a conta de serviço do projeto Firebase (`FIREBASE_SERVICE_ACCOUNT`).

---

## Mudanças no app

### Build e configuração
- `gradle/libs.versions.toml`, `build.gradle.kts`, `app/build.gradle.kts`:
  - Room 2.8.5 (`room-runtime`, `room-ktx`, `room-compiler` via KSP, `room.schemaLocation =
    app/schemas`)
  - Firebase BoM 34.19.0 + `firebase-messaging`
  - plugin `com.google.gms.google-services` 4.5.0
  - `room-testing` em `androidTest`
- `app/google-services.json` (**você fornece**: projeto Firebase com o app Android
  `com.raave.filament`). Sem ele o build falha, por isso é pré-requisito.
- `AndroidManifest.xml`:
  - permissão `POST_NOTIFICATIONS`;
  - `<service>` do `FilamentMessagingService` com o intent-filter `MESSAGING_EVENT`;
  - canais criados em código.

### Dados locais (novo: `data/local/`)
- `FilamentDatabase.kt`: Room v1, esquema exportado.
- `TicketEntity(id, title, statusName, statusId?, openedAt, dateMod, description?, unreadCount,
  lastReadFollowupId?)`.
- `MessageEntity(id PK, ticketId idx FK cascade, text, sentAt, authorName, isMine)`. A abertura
  **não** vira linha: continua derivada de `TicketEntity.description`, porque o id 0 colidiria entre
  chamados.
- `PendingReadEntity(ticketId PK, lastReadFollowupId)`: fila de marcações de lido ainda não
  confirmadas.
- DAOs:
  - `TicketDao`
    - `observeTickets()`: não lidas efetivas = 0 se houver pendente, senão `unreadCount`;
    - `observeTicket(id)`;
    - `replaceAll(list)`: apaga os ausentes e preserva `description`;
    - `updateStatus`;
    - `updateUnread`.
  - `MessageDao`
    - `observe(ticketId)`;
    - `upsertAll`;
    - `maxId(ticketId)`;
    - `replaceForTicket(ticketId, list)` (transação, usada na reconciliação).
  - `PendingReadDao`.
- `data/local/LocalMappers.kt`: entidade ↔ domínio.

### Rede e repositórios
- `data/glpi/GlpiRemoteDataSource.kt` (novo): as chamadas HTTP que hoje estão em
  `GlpiTicketRepository`, mais `after`, `markRead` e os campos de não lidas no parse
  (`GlpiMappers.kt`).
- `data/glpi/GlpiTicketRepository.kt`: vira coordenador local + remoto.
  - `observeTickets()` / `refreshTickets()`: primeiro esvazia a fila de lidos pendentes, depois busca a
    lista e grava.
  - `observeConversation(ticketId)`.
  - `syncMessages(ticketId, full)`:
    - `full=false` busca `after=maxId`;
    - sem mensagens locais, busca tudo;
    - `full=true` busca tudo e reconcilia (pega edição e exclusão feitas no GLPI).
  - `sendMessage` faz o POST e depois upsert.
  - `createTicket` faz o POST e depois `refreshTickets`.
  - `markRead(ticketId)`: grava o pendente, zera localmente e tenta enviar.
- `data/device/DeviceRegistrationRepository.kt` (novo): `POST /api/devices` com o token atual do FCM.
  Se falhar, marca em prefs para tentar de novo na próxima abertura.
- `di/RepositoryModule.kt` / `di/AppModule.kt`: binds novos, `@ApplicationScope CoroutineScope`,
  providers do Room (`databaseBuilder`, DAOs) e de `FirebaseMessaging`.

### Push e notificações (novo: `data/push/`, `ui/notification/`)
- `FilamentMessagingService` (`@AndroidEntryPoint`):
  - `onNewToken` → registro no backend;
  - `onMessageReceived` → `PushEventHandler`, ignorando se não houver sessão.
- `PushPayloadParser`: `Map<String, String>` → `PushEvent` selado (`FollowupCreated`, `StatusChanged`,
  `TicketRead`). Tipo desconhecido é ignorado.
- `PushEventHandler`: aplica no Room e decide notificar.
  - `FollowupCreated` → upsert da mensagem e `unreadCount` do payload. Com `contentTruncated`, dispara
    `syncMessages(after)`.
    - Se a conversa está visível: `markRead`, sem notificar.
    - Se `!isMine` e a mensagem ainda não existia no Room: notifica.
  - `StatusChanged` → atualiza o status e notifica.
  - `TicketRead` → atualiza a contagem e cancela a notificação do chamado.
- `ActiveConversationTracker` (singleton `StateFlow<Long?>`): o `ChatScreen` registra o chamado visível
  em resume/pause.
- `TicketNotifier`:
  - canais "Respostas dos chamados" e "Status dos chamados";
  - `MessagingStyle` por chamado (id da notificação = ticketId, várias respostas agrupadas);
  - conteúdo oculto na tela de bloqueio (`VISIBILITY_PRIVATE` + versão pública "Nova resposta no
    chamado #N");
  - toque → `PendingIntent` para a `MainActivity` com `ticketId`/`ticketTitle`;
  - `cancel(ticketId)` e `cancelAll()`.
- `data/session/SessionLifecycle.kt` (novo, iniciado em `FilamentApplication`): observa
  `AuthTokenStore.hasToken`.
  - Entrada → registra o dispositivo.
  - Saída → limpa o banco, cancela notificações e chama `FirebaseMessaging.deleteToken()`.

### Domínio
- `domain/model/Ticket.kt`: `+ unreadCount: Int = 0`.
- `domain/repository/TicketRepository.kt`: interface no estilo observar + sincronizar. Métodos:
  `observeTickets`, `refreshTickets`, `observeConversation`, `syncMessages`, `sendMessage`,
  `createTicket`, `markRead`.
- `domain/repository/DeviceRepository.kt` (novo).
- `domain/usecase/LoadConversationUseCase.kt` → **`ObserveConversationUseCase`**: combina o flow do
  chamado (abertura) com o flow das mensagens, na mesma regra de hoje.

### UI
- `ui/home/HomeViewModel.kt` / `HomeUiState.kt`:
  - coleta `observeTickets()`; `refreshTickets()` no início, ao retomar e no pull;
  - `ticketsBadgeCount` = soma das não lidas (`null` quando 0).
- `ui/home/HomeScreen.kt` (`TicketCard`): contagem de não lidas no card e título enfatizado quando > 0.
  Pedido de `POST_NOTIFICATIONS` logo após a primeira criação de chamado bem-sucedida, uma vez só (flag
  em prefs, que não é apagada ao sair).
- `ui/chat/ChatViewModel.kt` / `ChatUiState.kt`:
  - coleta `ObserveConversationUseCase`;
  - sync incremental ao abrir e retomar; pull-to-refresh = sync completo;
  - `markRead` quando a conversa está visível e o último id muda;
  - sem lista mutável nem eco manual.
- `ui/chat/ChatScreen.kt`: registra em `ActiveConversationTracker` e cancela a notificação do chamado ao
  abrir.
- `MainActivity.kt` / `MainViewModel.kt` / `ui/navigation/FilamentNavigation.kt`: intent de
  notificação (em `onCreate` e `onNewIntent`) vira um evento `openTicket`. A navegação empilha
  `ChatRoute` sobre `HomeRoute`, só com sessão.
- `res/values/strings.xml`: canais, textos de notificação e justificativa da permissão.

### Testes
- **JVM** (`app/src/test`):
  - `PushPayloadParserTest`;
  - `PushEventHandlerTest` (fakes de repositório, notifier e tracker: notifica / não notifica / isMine
    / conversa aberta / duplicado);
  - `HomeViewModelTest` (badge derivado);
  - `ChatViewModelTest` reescrito para flows (sync incremental vs completo, markRead ao ver);
  - `GlpiMappers`: novos campos e `after`.
- **Instrumentados** (`app/src/androidTest`, emulador):
  - DAOs com Room em memória: `replaceAll` preserva `description`, reconciliação por chamado, não
    lidas efetivas com pendente, cascade;
  - `GlpiTicketRepository` com Room real + remoto fake: esvaziamento da fila pendente, `after`
    ignorado pelo backend.

---

## Status da implementação

**Fase A concluída (2026-09-17):** testes unitários e instrumentados, mais E2E no emulador.
Diferenças em relação ao plano:
- **Room 3** (`androidx.room3` 3.0.3, driver `AndroidSQLiteDriver`) em vez de Room 2.8, porque é a linha
  estável recomendada. O banco é cache reconstruível, com migração destrutiva.
- **Conta guardada:** nome e e-mail ficam em `AuthTokenStore`, apagados junto com o token. Sem isso a
  Home mostrava a tela de erro sem rede e escondia o cache.
- **Limpeza do cache em dois momentos:** no login, antes de gravar o token (é o que garante a troca de
  conta), e ao perder a sessão.
- **Adiados para a fase B:** `unreadCount`/`lastReadFollowupId` vindos do backend e a fila
  `PendingReadEntity`.
- **`kotlinx-coroutines-android` 1.11.0 declarado explícito:** o app resolvia 1.9.0 por transitividade
  e os testes instrumentados quebravam.
- **Abertura da conversa à la Telegram (2026-09-17):** a conversa já abre posicionada, sem indicador de
  carregamento nem rolagem animada. O banco ganhou `tickets.lastReadMessageId` (versão 2), gravado por
  `advanceReadMarker` (só avança) conforme as mensagens aparecem acima da barra de mensagem, e a tela
  nasce no divisor "Novas mensagens" (primeira mensagem de outra pessoa depois do marcador) ou no fim.
  Sem marcador — chamado nunca aberto neste aparelho — não há "novas" e a conversa abre no fim. Isso
  antecipa parte da fase B: quando o backend expuser `lastReadFollowupId`, o marcador local passa a ser
  sincronizado em vez de só local.
- **Sincronização da conversa grava tudo numa transação:** antes, detalhe e mensagens iam em transações
  separadas e, na primeira abertura, a lista se montava só com a mensagem de abertura e depois rolava
  até o fim (o "flash" que se via ao abrir).
- **Testes instrumentados rodam via `adb`:** o `connectedDebugAndroidTest` falha nesta máquina (o UTP
  não consegue criar `C:\tmp`). Comandos:
  `./gradlew :app:assembleDebug :app:assembleDebugAndroidTest`, instalar os dois APKs e rodar
  `adb shell am instrument -w com.raave.filament.test/androidx.test.runner.AndroidJUnitRunner`.

## Ordem de execução
1. Enviar o contrato para a sessão do backbone.
2. **Fase A (independe do backend):** Room, repositórios offline-first, `ObserveConversation`, UI
   reativa, limpeza ao sair e testes. Entregável por si só: histórico persistido e só mensagens novas.
3. **Fase B (usa `unreadCount`/`read`):** badge por não lidas, contagem no card, fila de lidos pendentes.
   Funciona com badge zerado até o backend expor os campos.
4. **Fase C (precisa do `google-services.json` e do backend):** FCM, registro de dispositivo,
   notificações, toque → chat, permissão.

## Verificação
- `./gradlew :app:testDebugUnitTest` e `./gradlew :app:connectedDebugAndroidTest` (emulador).
- **E2E no emulador (Fase A):**
  - logar e abrir um chamado;
  - ligar modo avião e matar o app;
  - reabrir: lista e conversa aparecem do cache;
  - voltar à rede e enviar mensagem: aparece uma vez só;
  - pull-to-refresh reconcilia;
  - sair e logar com outra conta: nada da conta anterior aparece.
- **E2E com backbone (Fases B/C):**
  - responder pela UI do GLPI → notificação com o texto, badge +1, card com contagem;
  - tocar na notificação abre o chat e zera o badge; no segundo aparelho (Samsung) o badge também
    zera via `ticket.read`;
  - com o chat aberto, a resposta aparece ao vivo sem notificação;
  - mudar o status no GLPI → notificação de status e lista atualizada;
  - mensagem enviada pelo app → nenhuma notificação;
  - negar a permissão de notificação → badge e sync continuam funcionando.
- **Casos de borda:**
  - push chegando deslogado;
  - `contentTruncated`;
  - mesmo webhook entregue duas vezes (sem notificação duplicada);
  - backend sem as rotas novas (app não quebra);
  - latência do cron do GLPI.

## Riscos
- **Latência** do push = intervalo do cron do GLPI, medida em ~30–41 s. O app não depende de
  tempo real: sync ao abrir e retomar cobre o intervalo.
- `google-services.json` vira pré-requisito de build para qualquer pessoa que compile o app.
- Mensagens *data-only* de alta prioridade que não geram notificação (isMine, `ticket.read`) podem
  ser rebaixadas pelo Android se forem muito frequentes. O volume esperado é baixo.
