# Product

<!-- impeccable:product-schema 1 -->

## Platform

android

## Users

Colaboradores da Elinsa que precisam de suporte (TI e afins) e abrem chamados como **requerentes**. Usam o app para abrir um chamado, acompanhar a conversa com o time que atende e responder, inclusive com anexos. Técnicos e atendentes continuam no GLPI web; o app não atende esse papel.

## Product Purpose

O Filament é a porta de entrada mobile para os chamados da Elinsa, que vivem no GLPI. Existe por dois motivos:

- **Simplificar o GLPI:** o GLPI web é pesado e confuso no celular. No app, abrir e acompanhar um chamado leva poucos toques, num formato de conversa.
- **Levar o suporte para o canal oficial:** pedidos que hoje chegam por WhatsApp ou e-mail devem passar a ser registrados como chamado.

Sucesso é o colaborador preferir o app ao atalho informal, e o chamado registrado ser o caminho natural para pedir ajuda.

## Positioning

Não é outro sistema de chamados: é uma interface para o GLPI que a Elinsa já opera. Os chamados continuam no GLPI, onde o time já trabalha. O app só muda a experiência de quem pede ajuda.

## Operating Context

- Roda em **celulares pessoais (BYOD) e aparelhos corporativos gerenciados**, com Android variado, incluindo Samsung/One UI. Pode chegar a tablets.
- O app nunca fala com o GLPI diretamente. Tudo passa pelo **backbone** da Elinsa, que guarda as credenciais do GLPI e expõe `/api/glpi/*`.
- Login pelo backend Better Auth: código de 6 dígitos por e-mail, passkey ou conta Microsoft (via navegador).
- Idioma: português do Brasil.

## Capabilities and Constraints

- **Hoje:**
  - Abrir chamado (título + descrição).
  - Listar os próprios chamados.
  - Conversar num chamado (followups públicos do GLPI).
  - Anexar arquivos de até 15 MB.
- **Terminologia:** "chamado" (ticket do GLPI) e "mensagem" (followup). O status exibido é o nome já traduzido pelo GLPI.
- **Fora do app:** notas privadas do time técnico nunca aparecem para o requerente.
- **Em aberto:**
  - O contrato do upload de anexos ainda está em negociação com o backbone.
  - Ações do menu "⋮" do chat (ver detalhes, encerrar) ainda não foram definidas.
  - Notificações de resposta ainda não existem.

## Brand Commitments

- Nome do produto: **Filament**, para a **Elinsa**. Logo em `app/src/main/res/drawable/ic_logo.xml`.
- Tema no estilo shadcn/ui: paleta **"Mist"** (tokens em `ui/theme/Color.kt`, claro e escuro) e escala de raio do shadcn (`--radius` 10). Substituiu a paleta própria derivada do azul da Elinsa e o fundo preto AMOLED (decisão de 2026-09-17).
- Nunca dynamic color do Android.
- Transparência única no app: controles **flutuantes** (pílulas opacas com contorno de 1px, sem faixa de fundo) sobre o conteúdo, que ganha **blur progressivo + degradê** nas bordas onde passa por trás deles (status bar, barra de navegação, barras do chat). Padrão do repositório `sameerasw/android-common` (edge-to-edge + progressive blur).
- Componentes, estados de carregamento e animações seguem o padrão Compose/Material 3 (Expressive); só o tema é shadcn.

## Evidence on Hand

- Nenhum depoimento, métrica de adoção ou caso real foi fornecido. Trabalhos futuros não devem inventar números ou citações.

## Product Principles

1. **Mais simples que o GLPI, nunca um GLPI paralelo.** O app reduz passos; regras e dados continuam no GLPI.
2. **Conversa, não formulário.** Acompanhar um chamado deve parecer trocar mensagens com o suporte.
3. **Ser o caminho mais fácil.** Para vencer o WhatsApp e o e-mail, abrir um chamado precisa ser tão rápido quanto mandar uma mensagem.
4. **Funcionar em qualquer Android da frota.** Pessoal ou corporativo, barato ou topo de linha, com as limitações de fabricante tratadas no app.
