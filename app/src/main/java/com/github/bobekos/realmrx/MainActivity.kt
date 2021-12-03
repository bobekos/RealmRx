package com.github.bobekos.realmrx

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import io.realm.Realm
import io.realm.RealmConfiguration
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private val realmConfig = RealmConfiguration.Builder().name("test").build()

    private var realm: Realm? = null

    private var instanceDisposable: Disposable? = null
    private var singleShotDisposable: Disposable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        saveToDatabase()
        observeInstanceCount()

        findViewById<Button>(R.id.singleRxShotButton).setOnClickListener {
            executeSingleRxShot()
        }

        findViewById<Button>(R.id.closeRealmButton).setOnClickListener {
            realm?.close()
        }

        realm = Realm.getInstance(realmConfig)
    }

    override fun onPause() {
        super.onPause()

        singleShotDisposable?.dispose()
    }

    override fun onDestroy() {
        super.onDestroy()

        realm?.close()

        instanceDisposable?.dispose()
        instanceDisposable = null
    }

    private fun observeInstanceCount() {
        instanceDisposable = Observable.interval(2000, 2000, TimeUnit.MILLISECONDS)
            .map { Realm.getGlobalInstanceCount(realmConfig) }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                findViewById<TextView>(R.id.instanceCountTv).text = "global instance: $it"
            }
    }

    private fun executeSingleRxShot() {
        realm?.let { realm ->
            singleShotDisposable = realm.where(DummyModel::class.java)
                .equalTo("name", "Paul")
                .findAllAsync()
                .asFlowable()
                .observeOn(Schedulers.computation())
                .map { it[0]?.name ?: "no data found" }
                .firstOrError() //convert to single
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    Log.e("TEST", "JOB DONE BUT GLOBAL INSTANCE IS STILL 2")
                }, {
                    Log.e("TEST", "", it)
                });
        }
    }

    private fun saveToDatabase() {
        Completable.fromAction {
            val dummyData = listOf(
                DummyModel().apply {
                    this.name = "Paul"
                    this.state = "empty"
                },
                DummyModel().apply {
                    this.name = "Jake"
                    this.state = "doesn't matter"
                }
            )

            val realm = Realm.getDefaultInstance()
            realm.beginTransaction()
            realm.insertOrUpdate(dummyData)
            realm.commitTransaction()
            realm.close()
        }.subscribeOn(Schedulers.io()).subscribe()
    }
}