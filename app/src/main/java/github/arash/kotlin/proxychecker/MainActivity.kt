package github.arash.kotlin.proxychecker

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Bundle
import android.os.StrictMode
import android.util.Log
import android.view.*
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
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
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        AndroidNetworking.initialize(this)
        PreferenceHelper.initialize(this)

        StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder().permitAll().build())

        //Enter data from SharedPreferences
        binding.ProxyAddress.setText(PreferenceHelper.getInstance().getString(G.ProxyAddress_SharedPreferencesKey, ""))
        binding.ProxyPort.setText(PreferenceHelper.getInstance().getString(G.PortNumber_SharedPreferencesKey, ""))
        binding.ProxyUsername.setText(PreferenceHelper.getInstance().getString(G.Username_SharedPreferencesKey, ""))
        binding.ProxyPassword.setText(PreferenceHelper.getInstance().getString(G.Password_SharedPreferencesKey, ""))

        val connectivityManager: ConnectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_VPN)!!.isConnectedOrConnecting)
            binding.vpnAlertTV.visibility = VISIBLE

        var okHttpClient: OkHttpClient
        binding.StartTheTestButton.setOnClickListener {
            binding.ProgressBar.visibility = VISIBLE
            binding.StartTheTestButton.text = "Please Wait..."
            binding.StartTheTestButton.isEnabled = false

            //Proxy Type
            okHttpClient = if (binding.ProxyTypeToggleGroup.checkedButtonId == R.id.ProxyTypeHTTPS)
                OkHttpClient.Builder().proxy(Proxy(Proxy.Type.HTTP,
                    InetSocketAddress(binding.ProxyAddress.text.toString(),
                        binding.ProxyPort.text.toString().toInt()))).build()
            else
                OkHttpClient.Builder().proxy(Proxy(Proxy.Type.SOCKS,
                    InetSocketAddress(binding.ProxyAddress.text.toString(),
                        binding.ProxyPort.text.toString().toInt()))).build()

            //Proxy Credential
            Authenticator.setDefault(object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication? {
                    if (requestingHost.equals(binding.ProxyAddress.text.toString()))
                        if (requestingPort == binding.ProxyPort.text.toString().toInt())
                            return PasswordAuthentication(binding.ProxyUsername.text.toString(),
                                binding.ProxyPassword.text.toString().toCharArray()
                    )
                    return null
                }
            })

            //Make a Get Request
            AndroidNetworking.get("http://ip-api.com/json")
                .setOkHttpClient(okHttpClient)
                .build()
                .getAsJSONObject(object : JSONObjectRequestListener {
                    override fun onResponse(response: JSONObject?) {
                        Log.i("MainTAG", "Response: ${response.toString()}")
                        if (response!!.get("status").equals("success")) {

                            resultBottomSheet(response)

                            binding.ProgressBar.visibility = GONE
                            binding.StartTheTestButton.text = "Test The Proxy"
                            binding.StartTheTestButton.isEnabled = true
                        }
                    }

                    override fun onError(anError: ANError?) {
                        binding.ProgressBar.visibility = GONE
                        binding.StartTheTestButton.text = "Test The Proxy"
                        binding.StartTheTestButton.isEnabled = true
                        Snackbar.make(binding.root, "Connection failed", Snackbar.LENGTH_LONG)
                            .setAction("Copy info") { copyToClipboard(anError?.message.toString())}
                            .show()
                    }
                })

            PreferenceHelper.getInstance().setString(G.ProxyAddress_SharedPreferencesKey, binding.ProxyAddress.text.toString())
            PreferenceHelper.getInstance().setString(G.PortNumber_SharedPreferencesKey, binding.ProxyPort.text.toString())

            if (PreferenceHelper.getInstance().getBoolean(G.SaveCredential_SharedPreferencesKey)) {
                PreferenceHelper.getInstance()
                    .setString(G.Username_SharedPreferencesKey, binding.ProxyUsername.text.toString())
                PreferenceHelper.getInstance()
                    .setString(G.Password_SharedPreferencesKey, binding.ProxyPassword.text.toString())
            }
        }

        binding.SaveCredentialCheckBox.isChecked =
            PreferenceHelper.getInstance().getBoolean(G.SaveCredential_SharedPreferencesKey)
        binding.SaveCredentialCheckBox.setOnCheckedChangeListener { _, isChecked ->
            PreferenceHelper.getInstance()
                .setBoolean(G.SaveCredential_SharedPreferencesKey, isChecked)
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

            R.id.Action_ImportTelegramProxy -> importTelegramProxy()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun resultBottomSheet(response: JSONObject?){

        val resultBottomSheet = BottomSheetDialog(this@MainActivity)
        resultBottomSheet.setContentView(R.layout.design_result_btm)

        val resultFinalIP: TextView? =
            resultBottomSheet.findViewById(R.id.resultFinalIP)
        val resultCountry: TextView? =
            resultBottomSheet.findViewById(R.id.resultCountry)
        val resultCity: TextView? =
            resultBottomSheet.findViewById(R.id.resultCity)
        val resultISP: TextView? =
            resultBottomSheet.findViewById(R.id.resultISP)

        resultFinalIP?.text = response?.get("query").toString()
        resultCountry?.text = response?.get("country").toString()
        resultCity?.text = response?.get("city").toString()
        resultISP?.text = response?.get("isp").toString()

        resultFinalIP!!.setOnClickListener {
            copyToClipboard(response?.get("query").toString())
            Toast.makeText(this@MainActivity,"Copied",Toast.LENGTH_SHORT).show()
        }

        resultBottomSheet.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        resultBottomSheet.show()
    }

    private fun importTelegramProxy() {
        val inputDialog: AlertDialog = AlertDialog.Builder(this@MainActivity).create()
        val inflater: LayoutInflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view: View = inflater.inflate(R.layout.design_telegram_input_dialog, null)
        val telegramProxyInputBinding: DesignTelegramInputDialogBinding =
            DesignTelegramInputDialogBinding.bind(view)

        telegramProxyInputBinding.TelegramProxy.requestFocus()
        inputDialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)

        telegramProxyInputBinding.btnOK.setOnClickListener {
            if (telegramProxyInputBinding.TelegramProxy.text?.length!! < 5)
                telegramProxyInputBinding.TelegramProxyLayout.error = "Enter the proxy"
            else {
                try {
                    val myURI: Uri =
                        Uri.parse(telegramProxyInputBinding.TelegramProxy.text.toString())
                    binding.ProxyAddress.setText(myURI.getQueryParameter("server").toString())
                    binding.ProxyPort.setText(myURI.getQueryParameter("port").toString())

                    if (myURI.equals("username")) {
                        binding.ProxyUsername.setText(myURI.getQueryParameter("user").toString())
                        binding.ProxyPassword.setText(myURI.getQueryParameter("pass").toString())
                    }
                    inputDialog.dismiss()
                } catch (ex: Exception) {
                }
            }
        }
        telegramProxyInputBinding.btnCancel.setOnClickListener { inputDialog.dismiss() }
        inputDialog.setView(telegramProxyInputBinding.root)
        inputDialog.show()
    }

    fun copyToClipboard(content:String){
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Copied Text", content)
        clipboard.setPrimaryClip(clip)
    }

}