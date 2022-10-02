package top.xjunz.tasker.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import top.xjunz.tasker.Configurations
import top.xjunz.tasker.ktx.isTrue
import top.xjunz.tasker.service.OperatingMode
import top.xjunz.tasker.service.controller.ServiceController
import top.xjunz.tasker.service.controller.serviceController

/**
 * @author xjunz 2022/07/08
 */
class MainViewModel : ViewModel(), ServiceController.ServiceStateListener {

    val stopConfirmation = MutableLiveData<Boolean>()

    val isRunning = MutableLiveData(false)

    val isBinding = MutableLiveData<Boolean>()

    val bindingError = MutableLiveData<Throwable>()

    private val _operatingMode = MutableLiveData(OperatingMode.CURRENT)

    val operatingMode: LiveData<OperatingMode> get() = _operatingMode

    fun setCurrentOperatingMode(mode: OperatingMode) {
        serviceController.removeStateListener()
        Configurations.operatingMode = mode.VALUE
        serviceController.setStateListener(this)
        _operatingMode.value = mode
    }

    fun toggleService() {
        if (isRunning.isTrue) {
            serviceController.stopService()
        } else {
            serviceController.bindService()
        }
    }

    override fun onStartBinding() {
        isBinding.postValue(true)
    }

    override fun onError(t: Throwable) {
        isBinding.postValue(false)
        isRunning.postValue(false)
        bindingError.postValue(t)
    }

    override fun onServiceBound() {
        isBinding.postValue(false)
        isRunning.postValue(true)
    }

    override fun onServiceDisconnected() {
        isRunning.postValue(false)
    }
}