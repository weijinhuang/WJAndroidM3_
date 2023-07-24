package com.wj.androidm3.business.ui.main.fragment.dashboard

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.location.*
import android.os.Environment
import android.text.TextUtils
import android.view.WindowManager
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.messaging.FirebaseMessaging
import com.wj.androidm3.R
import com.wj.androidm3.business.services.BackgroundService
import com.wj.androidm3.business.ui.TestViewActivity
import com.wj.androidm3.business.ui.conversationincome.PhoneConversationActivity
import com.wj.androidm3.business.ui.kotlintest.KotlinTestActivity
import com.wj.androidm3.business.ui.media.MediaActivity
import com.wj.androidm3.business.ui.tabact.TabActivity
import com.wj.androidm3.databinding.FragmentDashboardBinding
import com.wj.basecomponent.ui.BaseMVVMFragment
import com.wj.basecomponent.util.log.WJLog
import com.wj.basecomponent.util.notification.sendNotification
import java.io.File
import java.io.FileOutputStream
import java.lang.ref.WeakReference
import java.util.*

class DashboardFragment : BaseMVVMFragment<DashboardViewModel, FragmentDashboardBinding>() {


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
    }

    private val mFunctionList = listOf(
        FunctionBean("StartNormalService") {
            val serviceIntent = Intent(requireActivity(), BackgroundService::class.java)
            requireActivity().startService(serviceIntent)
        },
        FunctionBean("SystemAlarmDialog") {
            val alertDialogBuilder = MaterialAlertDialogBuilder(requireActivity())
            alertDialogBuilder.setMessage("This is a SystemAlarmDialog")
            alertDialogBuilder.setTitle("SystemAlarmDialog")
            alertDialogBuilder.setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            alertDialogBuilder.setPositiveButton("Cancel") { dialog, _ -> dialog.dismiss() }
            val alertDialog = alertDialogBuilder.create()
            alertDialog.window?.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
            alertDialog.show()
        },
        FunctionBean("HeadsUpNotification") {
            sendNotification(
                requireActivity(),
                PhoneConversationActivity::class.java,
                "HWJ",
                1,
                R.mipmap.ic_launcher_round,
                "Title",
                "This is a heads up notification"
            )
        },
        FunctionBean("Get current FCM token") {
            FirebaseMessaging.getInstance().token.addOnCompleteListener {
                if (!it.isSuccessful) {
                    WJLog.e("Get current FCM token false : ${it.exception?.message ?: "error"}")
                }
                WJLog.i("current FCM token -> ${it.result}")
            }
        },
        FunctionBean("Install Google Play") {
            GoogleApiAvailability().makeGooglePlayServicesAvailable(requireActivity())
        },
        FunctionBean("Android Directory") {
            context?.let { ctx ->
                val fileName = "test.txt"

                WJLog.d("ctx.filesDir -> ${ctx.filesDir.absolutePath}")
                WJLog.d("ctx.cacheDir -> ${ctx.cacheDir.absolutePath}")
                WJLog.d("ctx.getExternalFilesDir(Environment.DIRECTORY_PICTURES) -> ${ctx.getExternalFilesDir(Environment.DIRECTORY_PICTURES)?.absolutePath}")
                WJLog.d("ctx.getExternalFilesDir(Environment.DIRECTORY_DCIM) -> ${ctx.getExternalFilesDir(Environment.DIRECTORY_DCIM)?.absolutePath}")
                WJLog.d("ctx.getExternalFilesDir(Environment.DIRECTORY_MOVIES) -> ${ctx.getExternalFilesDir(Environment.DIRECTORY_MOVIES)?.absolutePath}")
                WJLog.d("ctx.externalCacheDir -> ${ctx.externalCacheDir?.absolutePath}")
                WJLog.d("ctx.externalCacheDirs[0] -> ${ctx.externalCacheDirs[0].absolutePath}")
                WJLog.d("Environment.getDownloadCacheDirectory() -> ${Environment.getDownloadCacheDirectory().absolutePath}")
                WJLog.d("Environment.getExternalStorageDirectory() -> ${Environment.getExternalStorageDirectory().absolutePath}")
                WJLog.d("Environment.DIRECTORY_DCIM -> ${Environment.DIRECTORY_DCIM}")
            }

        },
        FunctionBean("Location") {
            context?.let { ctx ->
                val criteria = Criteria()
//                criteria.setAccuracy(Criteria.ACCURACY_FINE);
                //                criteria.setAccuracy(Criteria.ACCURACY_FINE);
                criteria.accuracy = Criteria.ACCURACY_COARSE
                criteria.isAltitudeRequired = false
                criteria.isBearingRequired = false
                criteria.isCostAllowed = false
                criteria.powerRequirement = Criteria.POWER_LOW
                val locationManager = context?.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                //String bestProvider = locationManager.getBestProvider(criteria, true);
                //Location lastKnownLocation = locationManager.getLastKnownLocation(bestProvider);
                //String bestProvider = locationManager.getBestProvider(criteria, true);
                //Location lastKnownLocation = locationManager.getLastKnownLocation(bestProvider);
                val lastKnownLocation: Location? = getLastKnownLocation(locationManager)
                val geocoder = Geocoder(requireActivity(), Locale.JAPAN)
                lastKnownLocation?.let {

                    val fromLocation = geocoder.getFromLocation(it.latitude, it.longitude, 1)
                    if (fromLocation.isNotEmpty()) {
                        val address = fromLocation.get(0)
                        WJLog.i("${address.countryName} ${address.locality} ${address.subLocality}  ")
                    }
                }
            }

        },
        FunctionBean("Test View") {
            requireActivity().startActivity(Intent(requireActivity(), TestViewActivity::class.java))
        },
        FunctionBean("TabActivity") {
            requireActivity().startActivity(Intent(requireActivity(), TabActivity::class.java))
        },
        FunctionBean("Native Test") {
//            WJLog.i("NativeLib().stringFromJNI() -> ${NativeLib().stringFromJNI()}")
        },
        FunctionBean("Kotlin Test") {
            Intent(requireActivity(), KotlinTestActivity::class.java).apply {
                requireActivity().startActivity(this)
            }
        },
        FunctionBean("Media Test") {
            Intent(requireActivity(), MediaActivity::class.java).apply {
                requireActivity().startActivity(this)
            }
        },
        FunctionBean("showDialog") {
            val title = arrayListOf<String>("base", "tip", "team", "self")
            showDialog("Title", "Content", "negative", "positive") { dialog, which ->
                WJLog.d("you click ${if (which == DialogInterface.BUTTON_NEGATIVE) "Negative" else "Positive"}")
                dialog.dismiss()
            }
        },
        FunctionBean("LocaleTest") {
            localeTest()
        },
        FunctionBean("viewpage2") {
            findNavController().navigate(R.id.viewPager2TestFragment)
        },
        FunctionBean("testLongSeViewPage2Fragment") {
            findNavController().navigate(R.id.testLongSeViewPage2Fragment)
        }
    )

    private fun localeTest() {
        val default = Locale.getDefault()
        WJLog.d("country:${default.country}\n language:${default.language} displayCountry:${default.displayCountry} displayLanguage:${default.displayLanguage} displayName:${default.displayName}")
    }

    /**
     * 进制转换测试
     */
    private fun calc() {
        val list4 = floatArrayOf(1200f, 234f, 234f, 4000f, 234f, 5037f, 21640f, 3049.99f)
        val list5 = floatArrayOf(53500f, 5500f, 3000f, 4000f, 0f, 507f, 2160f, 309.99f)
        val list6 = floatArrayOf(55200f, 51500f, 32000f, 40090f, 0f, 507f, 2160f, 309.99f)
        val list7 = floatArrayOf(1550110f, 200f, 300f, 400f, 0f, 507f, 2160f, 309.99f)
        val list8 = floatArrayOf(7800f, 9800f, 3000f, 4000f, 0f, 507f, 2160f, 309.99f)
        val list1 = floatArrayOf(5500f, 5500f, 3000f, 4000f, 0f, 507f, 2160f, 309.99f)
        val list3 = floatArrayOf(5800f, 5700f, 2560f, 3440f, 0f, 516.14f, 2160f, 294.72f)
        val list2 = floatArrayOf(5800f, 5700f, 3200f, 3440f, 640f, 516.14f, 2160f, 611.65f)
    }


    protected fun showDialog(
        title: String?, content: String?,
        negativeText: CharSequence,
        positiveText: CharSequence?, onClickListener: DialogInterface.OnClickListener?
    ) {
        val builder = MaterialAlertDialogBuilder(requireActivity())
        if (!TextUtils.isEmpty(title)) {
            builder.setTitle(title)
        }
        if (!TextUtils.isEmpty(content)) {
            builder.setMessage(content)
        }
        if (!TextUtils.isEmpty(negativeText)) {
            builder.setNegativeButton(negativeText, onClickListener)
        }
        if (!TextUtils.isEmpty(positiveText)) {
            builder.setPositiveButton(positiveText, onClickListener)
        }

        val alertDialog = builder.create()
        alertDialog.show()
    }

    private fun makeFile(fileName: String, dir: String) {
        val file = File(dir + File.separator + fileName)
        val dirFile = File(dir)
        if (!dirFile.exists()) {
            dirFile.mkdirs()
        }
        FileOutputStream(file).use {
            it.write("this is a test file".toByteArray())
        }
    }

    fun getLastKnownLocation(locationManager: LocationManager): Location? {
        val providers = locationManager.allProviders
        var bestLocation: Location? = null
        for (provider in providers) {
            @SuppressLint("MissingPermission") val location = locationManager.getLastKnownLocation(provider) ?: continue
            location?.let { l ->
                if (bestLocation == null) {
                    // Found best last known location: %s", l);
                    bestLocation = l
                }
            }

        }
        return bestLocation
    }

    override fun firstCreateView() {
        val adapter = DashboardRecyclerViewAdapter(mFunctionList)
        mViewBinding?.run {
            functionRv.adapter = adapter
            functionRv.layoutManager = LinearLayoutManager(requireActivity(), LinearLayoutManager.VERTICAL, false)
        }
    }


    override fun getLayoutId(): Int {
        return R.layout.fragment_dashboard
    }
}