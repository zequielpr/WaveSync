package com.kunano.wavesynch

import android.annotation.SuppressLint
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kunano.wavesynch.MainActivity.RoomNavArgs
import com.kunano.wavesynch.data.data_store_preferences.DataStorePreferences
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainActivityViewModel @Inject constructor(@ApplicationContext private val context: Context) :
    ViewModel() {

    private var _pendingRoomNavFlow = MutableStateFlow<RoomNavArgs?>(null)
    val pendingRoomNavFlow = _pendingRoomNavFlow.asStateFlow()

    var sharedPreferences: SharedPreferences? = null


    init {
        sharedPreferences = context.getSharedPreferences("wavesync_prefs", MODE_PRIVATE)
    }

    fun getIsFirstOpening(): Boolean{
        return sharedPreferences?.getBoolean("is_first_opening", true) ?: true
    }

    @SuppressLint("UseKtx")
    fun setIsFirstOpening(value: Boolean){
        sharedPreferences?.edit()?.putBoolean("is_first_opening", value)?.apply()

    }

    fun setPrivacyAndPoliciesAccepted(value: Boolean){
        sharedPreferences?.edit()?.putBoolean("privacy_and_policies_accepted", value)?.apply()

    }

    fun getPrivacyAndPoliciesAccepted(): Boolean{
        return sharedPreferences?.getBoolean("privacy_and_policies_accepted", false) ?: false
    }




    fun handleIntent(intent: Intent) {
        val roomName = intent.getStringExtra("roomName")
        val hostName = intent.getStringExtra("hostName")
        if (roomName != null && hostName != null) {
            viewModelScope.launch {
                _pendingRoomNavFlow.emit(RoomNavArgs(roomName, hostName))
            }
        }

    }

    fun emptyRoomNavArgs() {
        viewModelScope.launch {
            _pendingRoomNavFlow.emit(null)
        }

    }
}