package github.arash.kotlin.proxychecker

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.CompoundButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.JSONObjectRequestListener
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import github.arash.kotlin.proxychecker.databinding.ActivityMainBinding
import github.arash.kotlin.proxychecker.databinding.DesignSettingBtmBinding
import github.arash.kotlin.proxychecker.databinding.DesignTelegramInputDialogBinding
import okhttp3.OkHttpClient
import org.json.JSONObject
import java.lang.Exception
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

        binding.ProxyAddress.setText(
            PreferenceHelper.getInstance().getString(G.ProxyAddress_SharedPreferencesKey, "")
        )
        binding.ProxyPort.setText(
            PreferenceHelper.getInstance().getString(G.PortNumber_SharedPreferencesKey, "")
        )
        binding.ProxyUsername.setText(
            PreferenceHelper.getInstance().getString(G.Username_SharedPreferencesKey, "")
        )
        binding.ProxyPassword.setText(
            PreferenceHelper.getInstance().getString(G.Password_SharedPreferencesKey, "")
        )

        PreferenceHelper.getInstance().getString(G.PortNumber_SharedPreferencesKey, "")
        PreferenceHelper.getInstance().getString(G.Username_SharedPreferencesKey, "")
        PreferenceHelper.getInstance().getString(G.Password_SharedPreferencesKey, "")


        val connectivityManager: ConnectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_VPN)!!.isConnectedOrConnecting)
            binding.vpnAlertTV.visibility = VISIBLE

        var okHttpClient: OkHttpClient
        binding.StartTheTestButton.setOnClickListener {
            binding.ProgressBar.visibility = VISIBLE
            binding.StartTheTestButton.text = "Please Wait..."
            binding.StartTheTestButton.isEnabled = false

            //Proxy Type
            if (binding.ProxyTypeToggleGroup.checkedButtonId == R.id.ProxyTypeHTTPS)
                okHttpClient = OkHttpClient.Builder()
                    .proxy(
                        Proxy(
                            Proxy.Type.HTTP, InetSocketAddress(
                                binding.ProxyAddress.text.toString(),
                                binding.ProxyPort.text.toString().toInt()
                            )
                        )
                    ).build()
            else
                okHttpClient = OkHttpClient.Builder().proxy(
                    Proxy(
                        Proxy.Type.SOCKS, InetSocketAddress(
                            binding.ProxyAddress.text.toString(),
                            binding.ProxyPort.text.toString().toInt()
                        )
                    )
                ).build()

            //Proxy Credential
            Authenticator.setDefault(object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication? {
                    if (requestingHost.equals(
                            binding.ProxyAddress.text.toString(),
                        )
                    ) if (binding.ProxyPort.text.toString()
                            .toInt() == requestingPort
                    ) return PasswordAuthentication(
                        binding.ProxyUsername.text.toString(),
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
                        Log.i("TheMainTAG", "Response: ${response.toString()}")
                        if (response!!.get("status").equals("success")) {
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

                            resultFinalIP?.text = response.get("query").toString()
                            resultCountry?.text = response.get("country").toString()
                            resultCity?.text = response.get("city").toString()
                            resultISP?.text = response.get("isp").toString()
                            resultBottomSheet.behavior.state = BottomSheetBehavior.STATE_EXPANDED
                            resultBottomSheet.show()
                            binding.ProgressBar.visibility = GONE
                            binding.StartTheTestButton.text = "Test The Proxy"
                            binding.StartTheTestButton.isEnabled = true
                        }
                    }

                    override fun onError(anError: ANError?) {
                        binding.ProgressBar.visibility = GONE
                        binding.StartTheTestButton.text = "Test The Proxy"
                        binding.StartTheTestButton.isEnabled = true
                    }
                })

            PreferenceHelper.getInstance().setString(
                G.ProxyAddress_SharedPreferencesKey,
                binding.ProxyAddress.text.toString()
            )
            PreferenceHelper.getInstance()
                .setString(G.PortNumber_SharedPreferencesKey, binding.ProxyPort.text.toString())

            if (PreferenceHelper.getInstance().getBoolean(G.SaveCredential_SharedPreferencesKey)) {
                PreferenceHelper.getInstance()
                    .setString(
                        G.Username_SharedPreferencesKey,
                        binding.ProxyUsername.text.toString()
                    )
                PreferenceHelper.getInstance()
                    .setString(
                        G.Password_SharedPreferencesKey,
                        binding.ProxyPassword.text.toString()
                    )
            }
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
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://github.com/MrArashAzizi/Proxy-Checking-Tool")
                    )
                )
            R.id.Action_Setting -> settingBottomSheetDialog()

            R.id.Action_ImportTelegramProxy -> importTelegramProxy()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun settingBottomSheetDialog() {
        val settingBTM = BottomSheetDialog(this)
        val inflater: LayoutInflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view: View = inflater.inflate(R.layout.design_setting_btm, null)
        val settingBottomSheetBinding: DesignSettingBtmBinding =
            DesignSettingBtmBinding.bind(view)

        settingBottomSheetBinding.SettingSaveCredential.isChecked =
            PreferenceHelper.getInstance().getBoolean(G.SaveCredential_SharedPreferencesKey)
        settingBottomSheetBinding.SettingSaveCredential.setOnCheckedChangeListener { _, isChecked ->
            PreferenceHelper.getInstance()
                .setBoolean(G.SaveCredential_SharedPreferencesKey, isChecked)
        }

        settingBTM.setContentView(view)
        settingBTM.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        settingBTM.show()
    }

    private fun importTelegramProxy() {
        val inputDialog: AlertDialog = AlertDialog.Builder(this@MainActivity).create()
        val inflater: LayoutInflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view: View = inflater.inflate(R.layout.design_telegram_input_dialog, null)
        val telegramProxyInputBinding: DesignTelegramInputDialogBinding =
            DesignTelegramInputDialogBinding.bind(view)

        telegramProxyInputBinding.btnOK.setOnClickListener {
            try {

                val myURI: Uri = Uri.parse(telegramProxyInputBinding.TelegramProxy.text.toString())
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
        telegramProxyInputBinding.btnCancel.setOnClickListener { inputDialog.dismiss() }
        inputDialog.setView(telegramProxyInputBinding.root)
        inputDialog.show()
    }

}