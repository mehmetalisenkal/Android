/*
 * Copyright (c) 2017 DuckDuckGo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.duckduckgo.app.browser

import android.net.Uri
import com.duckduckgo.app.global.AppUrl.ParamKey
import com.duckduckgo.app.global.AppUrl.ParamValue
import com.duckduckgo.app.statistics.store.StatisticsDataStore
import timber.log.Timber

interface RequestRewriter {
    fun shouldRewriteRequest(uri: Uri): Boolean
    fun rewriteRequestWithCustomQueryParams(request: Uri): Uri
    fun addCustomQueryParams(builder: Uri.Builder)
}

class DuckDuckGoRequestRewriter(private val duckDuckGoUrlDetector: DuckDuckGoUrlDetector, private val statisticsStore: StatisticsDataStore) :
    RequestRewriter {

    override fun rewriteRequestWithCustomQueryParams(request: Uri): Uri {
        val builder = Uri.Builder()
            .authority(request.authority)
            .scheme(request.scheme)
            .path(request.path)
            .fragment(request.fragment)

        request.queryParameterNames
            .filter { it != ParamKey.SOURCE && it != ParamKey.APP_VERSION && it != ParamKey.ATB }
            .forEach { builder.appendQueryParameter(it, request.getQueryParameter(it)) }

        addCustomQueryParams(builder)
        val newUri = builder.build()

        Timber.d("Rewriting request\n$request [original]\n$newUri [rewritten]")
        return newUri
    }

    override fun shouldRewriteRequest(uri: Uri): Boolean {
        return duckDuckGoUrlDetector.isDuckDuckGoQueryUrl(uri.toString()) &&
                !uri.queryParameterNames.containsAll(arrayListOf(ParamKey.SOURCE, ParamKey.APP_VERSION, ParamKey.ATB))
    }

    /**
     * Applies cohort (atb) https://duck.co/help/privacy/atb, app version and
     * and source (t) https://duck.co/help/privacy/t params to url
     */
    override fun addCustomQueryParams(builder: Uri.Builder) {
        val atb = statisticsStore.atb
        if (atb != null) {
            builder.appendQueryParameter(ParamKey.ATB, atb)
        }
        builder.appendQueryParameter(ParamKey.APP_VERSION, ParamValue.appVersion)
        builder.appendQueryParameter(ParamKey.SOURCE, ParamValue.SOURCE)
    }
}
