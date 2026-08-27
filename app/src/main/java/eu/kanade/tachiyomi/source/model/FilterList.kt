// Created by Notch
package eu.kanade.tachiyomi.source.model

class FilterList(val list: List<Filter<*>>) : List<Filter<*>> by list {
    constructor(vararg filters: Filter<*>) : this(filters.asList())
}
