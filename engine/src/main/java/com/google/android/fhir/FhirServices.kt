/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.fhir

import android.content.Context
import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import ca.uhn.fhir.parser.IParser
import com.google.android.fhir.db.Database
import com.google.android.fhir.db.impl.DatabaseConfig
import com.google.android.fhir.db.impl.DatabaseEncryptionKeyProvider.isDatabaseEncryptionSupported
import com.google.android.fhir.db.impl.DatabaseImpl
import com.google.android.fhir.impl.FhirEngineImpl
import com.google.android.fhir.sync.DataSource
import com.google.android.fhir.sync.remote.RemoteFhirService
import timber.log.Timber

internal data class FhirServices(
  val fhirEngine: FhirEngine,
  val parser: IParser,
  val database: Database,
  val remoteDataSource: DataSource? = null
) {
  class Builder(private val context: Context) {
    private var inMemory: Boolean = false
    private var enableEncryption: Boolean = false
    private var databaseErrorStrategy = DatabaseErrorStrategy.UNSPECIFIED
    private var serverConfiguration: ServerConfiguration? = null

    internal fun inMemory() = apply { inMemory = true }

    internal fun enableEncryptionIfSupported() = apply {
      if (!isDatabaseEncryptionSupported()) {
        Timber.w("Database encryption isn't supported in this device.")
        return this
      }
      enableEncryption = true
    }

    internal fun setDatabaseErrorStrategy(databaseErrorStrategy: DatabaseErrorStrategy) {
      this.databaseErrorStrategy = databaseErrorStrategy
    }

    internal fun setServerConfiguration(serverConfiguration: ServerConfiguration) {
      this.serverConfiguration = serverConfiguration
    }

    fun build(): FhirServices {
      val parser = FhirContext.forCached(FhirVersionEnum.R4).newJsonParser()
      val db =
        DatabaseImpl(
          context = context,
          iParser = parser,
          DatabaseConfig(inMemory, enableEncryption, databaseErrorStrategy)
        )
      val engine = FhirEngineImpl(database = db, context = context)
      val remoteDataSource =
        serverConfiguration?.let {
          RemoteFhirService.builder(it.baseUrl).apply { setAuthenticator(it.authenticator) }.build()
        }
      return FhirServices(
        fhirEngine = engine,
        parser = parser,
        database = db,
        remoteDataSource = remoteDataSource
      )
    }
  }

  companion object {
    fun builder(context: Context) = Builder(context)
  }
}
