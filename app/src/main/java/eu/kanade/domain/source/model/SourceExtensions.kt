package eu.kanade.domain.source.model

import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.extension.model.Extension
import tachiyomi.domain.source.model.Source
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

val Source.installedExtension: Extension.Installed?
    get() {
        return Injekt.get<ExtensionManager>()
            .installedExtensionsFlow
            .value
            .find { ext -> ext.sources.any { it.id == id } }
    }
