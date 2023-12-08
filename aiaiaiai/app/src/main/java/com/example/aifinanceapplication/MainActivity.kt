package com.example.aifinanceapplication
import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : Activity() {

    private var sum: Double = 0.0
    private val sumKey = "sum_key"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val askAdviceButton: Button = findViewById(R.id.askAdviceButton)
        val counterTextView: TextView = findViewById(R.id.counterTextView)
        loadSum()
        updateSumText(counterTextView)
        askAdviceButton.setOnClickListener {
            askForAdvice()
            addIncomeAndSubtractSpending()
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun askForAdvice() {
        // Retrieve user's monthly income from your input field
        val incomeInput: EditText = findViewById(R.id.incomeInput)
        val spendingInput: EditText = findViewById(R.id.spendingInput)
        val userMonthlyIncome = incomeInput.text.toString()
        val userMonthlySpending = spendingInput.text.toString()

        // Send user input to Wit.ai for processing using coroutines
        GlobalScope.launch(Dispatchers.Main) {
            val advice = withContext(Dispatchers.IO) {
                makeWitRequest(userMonthlyIncome,userMonthlySpending)
            }

            // Update your UI or take further actions based on the advice
            val adviceText: TextView = findViewById(R.id.adviceText)
            adviceText.text = advice
            val counterTextView: TextView = findViewById(R.id.counterTextView)
            updateSumText(counterTextView)
        }
    }

    private suspend fun makeWitRequest(userMonthlyIncome: String,userMonthlySpending: String): String {
        val witToken = "HKGV6QRUFBBXZVBAKTPS4QY236ZAPH4I" // Replace with your Wit.ai server token

        return try {
            // Construct the URL for the Wit.ai API
            val witUrl = "https://api.wit.ai/message?v=20231207&q=I%20spend%20$userMonthlySpending%20huf%20with%20$userMonthlyIncome%20huf%20as%20my%20monthly%20salary"
            val url = URL(witUrl)
            val urlConnection = url.openConnection() as HttpURLConnection

            // Set headers
            urlConnection.setRequestProperty("Authorization", "Bearer $witToken")

            // Get the response
            val reader = BufferedReader(InputStreamReader(urlConnection.inputStream))
            val sb = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                sb.append(line).append("\n")
            }
            reader.close()

            val jsonResponse = JSONObject(sb.toString())
            val intents = jsonResponse.getJSONArray("intents")

            var returntext="couldn't get advice"
            if (intents != null && intents.length() > 0) {
                // Handle each recognized intent
                for (i in 0 until intents.length()) {
                    val intentObject = intents.getJSONObject(i)
                    val intentName = intentObject.optString("name", "Unknown")
                    if(intentName=="wantstoinvest"){
                        returntext="You want to invest your money"
                    }
                    if(intentName=="wantstospendless"){
                        returntext="You want to reduce your spending"
                    }
                    if(intentName=="wantstospendmore"){
                        returntext="You want to increase your spending"
                    }

                    // Handle the intent based on your logic
                }
            } else {
                // No intents recognized
                "Unable to get advice"
            }



            returntext
        } catch (e: Exception) {
            e.printStackTrace()
            "Unable to get advice"
        }
    }

    private fun addIncomeAndSubtractSpending() {
        // Retrieve user's monthly income and spending from input fields
        val incomeInput: EditText = findViewById(R.id.incomeInput)
        val spendingInput: EditText = findViewById(R.id.spendingInput)

        val userMonthlyIncome = incomeInput.text.toString().toDoubleOrNull() ?: 0.0
        val userSpending = spendingInput.text.toString().toDoubleOrNull() ?: 0.0

        // Update the sum value
        sum += userMonthlyIncome - userSpending

        // Save the updated sum value
        saveSum()
    }

    private fun updateSumText(counterTextView: TextView) {
        // Display the updated sum value
        val result = "Sum: $sum"
        counterTextView.text = result
    }

    private fun saveSum() {
        val sharedPreferences = getPreferences(Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putFloat(sumKey, sum.toFloat())
        editor.apply()
    }

    private fun loadSum() {
        val sharedPreferences = getPreferences(Context.MODE_PRIVATE)
        sum = sharedPreferences.getFloat(sumKey, 0.0f).toDouble()
    }

    override fun onStop() {
        super.onStop()
        // Save the sum value when the app closes
        saveSum()
    }
}