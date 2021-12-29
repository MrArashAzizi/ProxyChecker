package github.arash.kotlin.proxychecker

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.JSONObjectRequestListener
import github.arash.kotlin.proxychecker.databinding.DesignMyIpDialogBinding
import org.json.JSONObject

class MyIpDialog {
    fun show(context: Context){
        val progressDialog = ProgressDialog(context, R.style.AppCompatAlertDialogStyle)
        progressDialog.setCancelable(false)
        progressDialog.setMessage("Please wait...")
        progressDialog.show()

        val ipDialog: AlertDialog = AlertDialog.Builder(context).create()
        val inflater:LayoutInflater = context.getSystemService(AppCompatActivity.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view: View = inflater.inflate(R.layout.design_my_ip_dialog, null)
        val vBinding: DesignMyIpDialogBinding = DesignMyIpDialogBinding.bind(view)
        ipDialog.setView(vBinding.root)

        AndroidNetworking.get("http://ip-api.com/json")
            .doNotCacheResponse()
            .build()
            .getAsJSONObject(object : JSONObjectRequestListener{
                @SuppressLint("SetTextI18n")
                override fun onResponse(response: JSONObject?) {
                    if (response!!.get("status").equals("success")) {

                        vBinding.apply {
                            mIPAddress.text = response.getString("query")
                            mIPLocation.text =  "${response.getString("country")} | ${response.getString("city")}"
                            mIPTimezone.text = response.getString("timezone")
                            mIPISP.text = response.getString("isp")
                        }

                        progressDialog.dismiss()
                        ipDialog.show()
                    }
                }

                override fun onError(anError: ANError?) {
                    vBinding.mIPProgressView.visibility = View.GONE
                }

            })
        vBinding.btnOK.setOnClickListener { ipDialog.dismiss() }
    }

}