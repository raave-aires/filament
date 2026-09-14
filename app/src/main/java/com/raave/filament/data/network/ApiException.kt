package com.raave.filament.data.network

import java.io.IOException

/** Erro HTTP com corpo de resposta já interpretado (status != 2xx). */
class ApiException(val statusCode: Int, message: String) : IOException(message)
