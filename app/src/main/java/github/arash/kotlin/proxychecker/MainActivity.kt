package github.arash.kotlin.proxychecker

import android.app.AlertDialog
import android.content.*
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Bundle
import android.os.StrictMode
import android.util.Log
import android.view.*
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.JSONObjectRequestListener
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import github.arash.kotlin.proxychecker.databinding.ActivityMainBinding
import github.arash.kotlin.proxychecker.databinding.DesignTelegramInputDialogBinding
import okhttp3.OkHttpClient
import org.json.JSONObject
import java.net.Authenticator
import java.net.InetSocketAddress
import java.net.PasswordAuthentication
import java.net.Proxy

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsFragment())
                .commit()
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        binding.apply {
            setContentView(root)
            setSupportActionBar(toolbar)
            val preferenceManager = PreferenceManager.getDefaultSharedPreferences(this@MainActivity)

            AndroidNetworking.initialize(this@MainActivity)

            StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder().permitAll().build())

            val connectivityManager: ConnectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_VPN)?.let {
                if(preferenceManager.getBoolean("VPN_ALERT", true) && it.isConnectedOrConnecting)
                    binding.vpnAlertTV.visibility = View.VISIBLE
            }

            var okHttpClient: OkHttpClient
            btnGO.setOnClickListener {
                try {

                    ProgressBar.visibility = View.VISIBLE
                    btnGO.text = "Please Wait..."
                    btnGO.isEnabled = false

                    val proxyAddress: String? = preferenceManager.getString("ProxyServer", null)
                    val proxyPort: String? = preferenceManager.getString("RemotePort", null)

                    //Proxy Type with okHttpClient
                    okHttpClient = OkHttpClient.Builder().proxy(Proxy(
                        when (preferenceManager.getString("ProxyType", "socks5")) {
                            "socks5" -> Proxy.Type.SOCKS
                            else -> Proxy.Type.HTTP
                        }, InetSocketAddress(proxyAddress, proxyPort!!.toInt()))).build()

                    //Proxy Credential
                    Authenticator.setDefault(object : Authenticator() {
                        override fun getPasswordAuthentication(): PasswordAuthentication? {
                            if (requestingHost.equals(proxyAddress))
                                if (requestingPort ==  proxyPort.toInt())
                                    return PasswordAuthentication(preferenceManager.getString("CredentialUsername", null),
                                        preferenceManager.getString("CredentialPassword", null)?.toCharArray())
                            return null
                        }
                    })

                    val startTime = System.currentTimeMillis()
                    //Make a Get Request
                    AndroidNetworking.get("http://ip-api.com/json")
                        .setOkHttpClient(okHttpClient)
                        .doNotCacheResponse()
                        .build()
                        .getAsJSONObject(object : JSONObjectRequestListener {
                            override fun onResponse(response: JSONObject?) {
                                Log.i("MainTAG", "Response: ${response.toString()}")
                                if (response!!.get("status").equals("success")) {
                                    val endTime = System.currentTimeMillis()
                                    resultBottomSheet(response, endTime - startTime)
                                    ProgressBar.visibility = View.GONE
                                    btnGO.text = "Start Checking"
                                    btnGO.isEnabled = true
                                }
                            }

                            override fun onError(anError: ANError?) {
                                ProgressBar.visibility = View.GONE
                                btnGO.text = "Start Checking"
                                btnGO.isEnabled = true
                                Snackbar.make(root, "Connection failed | ${anError?.errorCode}", Snackbar.LENGTH_LONG).setAnchorView(btnGO)
                                    .setAction("Copy info") { copyToClipboard(anError?.message.toString())}
                                    .show()
                            }
                        })

                }catch (ex:Exception){
                    ex.printStackTrace()
                    ProgressBar.visibility = View.GONE
                    btnGO.text = "Start Checking"
                    btnGO.isEnabled = true
                    Snackbar.make(root, "ERROR!", Snackbar.LENGTH_LONG).setAnchorView(btnGO)
                        .setAction("Copy info") { copyToClipboard(ex.toString())}
                        .show()
                }
            }
        }

    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
        }

        override fun onPreferenceTreeClick(preference: Preference?): Boolean {
            when(preference?.key){
                "MoreMY_IP" -> MyIpDialog().show(requireContext())
            }
            return super.onPreferenceTreeClick(preference)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.toolbar_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.Action_Github ->
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.github_address))))
            R.id.Action_ImportTelegramProxy ->
                importTelegramProxy()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun resultBottomSheet(response: JSONObject?, elapsedTime: Long){
        BottomSheetDialog(this@MainActivity).apply {
            setContentView(R.layout.design_result_btm)

            val resultElapsedTime: TextView? = findViewById(R.id.resultElapsedTime)
            val resultFinalIP: TextView? = findViewById(R.id.resultFinalIP)
            val resultCountry: TextView? = findViewById(R.id.resultCountry)
            val resultCity: TextView? = findViewById(R.id.resultCity)
            val resultISP: TextView? = findViewById(R.id.resultISP)

            resultElapsedTime?.text = "Data received in ${elapsedTime}ms."
            resultFinalIP?.text = response?.get("query").toString()
            resultCountry?.text = response?.get("country").toString()
            resultCity?.text = response?.get("city").toString()
            resultISP?.text = response?.get("isp").toString()

            resultFinalIP!!.setOnClickListener {
                copyToClipboard(response?.get("query").toString())
                Toast.makeText(this@MainActivity,"Copied", Toast.LENGTH_SHORT).show()
            }

            behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }.show()
    }

    private fun importTelegramProxy() {
        val preferenceManager = PreferenceManager.getDefaultSharedPreferences(this)
        val editor: SharedPreferences.Editor = preferenceManager.edit()

        val inputDialog: AlertDialog = AlertDialog.Builder(this@MainActivity).create()
        val inflater: LayoutInflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view: View = inflater.inflate(R.layout.design_telegram_input_dialog, null)
        val telegramProxyInputBinding: DesignTelegramInputDialogBinding =
            DesignTelegramInputDialogBinding.bind(view)

        telegramProxyInputBinding.apply {
            TelegramProxy.requestFocus()
            inputDialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)

            btnOK.setOnClickListener {
                if (TelegramProxy.text?.length!! < 5)
                    TelegramProxyLayout.error = "Enter the proxy"
                else {
                    try {
                        val myURI: Uri = Uri.parse(telegramProxyInputBinding.TelegramProxy.text.toString())

                        editor.apply{
                            putString("ProxyServer", myURI.getQueryParameter("server").toString())
                            putString("RemotePort", myURI.getQueryParameter("port").toString())
                            if (myURI.equals("username")) {
                                putString("CredentialUsername", myURI.getQueryParameter("user").toString())
                                putString("CredentialPassword", myURI.getQueryParameter("pass").toString())
                            }
                        }.apply()

                        inputDialog.dismiss()
                        recreate() // MainActivity
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }
                }
            }
            btnCancel.setOnClickListener { inputDialog.dismiss() }
        }

        inputDialog.setView(telegramProxyInputBinding.root)
        inputDialog.show()
    }

    fun copyToClipboard(content:String){
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Copied Text", content)
        clipboard.setPrimaryClip(clip)
    }
}