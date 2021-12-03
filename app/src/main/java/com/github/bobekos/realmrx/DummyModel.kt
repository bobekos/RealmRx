package com.github.bobekos.realmrx

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey


open class DummyModel : RealmObject() {
    @PrimaryKey
    var name: String = ""
    var state: String? = null
}