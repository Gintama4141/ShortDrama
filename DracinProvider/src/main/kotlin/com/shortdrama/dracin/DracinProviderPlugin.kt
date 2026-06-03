package com.shortdrama.dracin

import android.content.Context
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class DracinProviderPlugin : Plugin() {
    override fun load(context: Context) {
        registerMainAPI(DramaboxProvider())
        registerMainAPI(ReelshortProvider())
        registerMainAPI(FlickreelsProvider())
        registerMainAPI(DramawaveProvider())
        registerMainAPI(GoodshortProvider())
        registerMainAPI(NetshortProvider())
        registerMainAPI(IdramaProvider())
        registerMainAPI(StardusttvProvider())
        registerMainAPI(DramabiteProvider())
        registerMainAPI(ShortmaxProvider())
    }
}
