package com.example.myapplication

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import com.dropbox.android.external.store4.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.firstOrNull
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private val store = StoreBuilder.from<Int, String>(Fetcher.of { key ->
        delay(2000) //simulate long network request
        "$key :: " + Random.nextInt()
    }).build()

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onStart() {
        super.onStart()
        val out = findViewById<TextView>(R.id.out)

        findViewById<TextView>(R.id.but_refresh)?.setOnClickListener {
            scope.launch {
                while(isActive) {
                    store.stream(StoreRequest.fresh(1)).filterNot { it is StoreResponse.Loading }.firstOrNull()?.dataOrNull()
                    delay(500)
                }
            }

            scope.launch {
                store.stream(StoreRequest.cached(1, false)).collect {
                    withContext(Dispatchers.Main) {
                        out?.text = it.toString()
                    }
                }
            }

            (1..20).onEach { index ->
                scope.launch {
                    store.stream(StoreRequest.cached(1, false)).collect {
                        println("$index :: $it")
                    }
                }
            }
        }

        findViewById<TextView>(R.id.but_cancel)?.setOnClickListener {
            scope.coroutineContext.cancelChildren()
        }
    }
}
