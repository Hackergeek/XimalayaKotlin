package com.rickon.ximalayakotlin.fragment

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Message
import androidx.recyclerview.widget.LinearLayoutManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.rickon.ximalayakotlin.R
import com.rickon.ximalayakotlin.adapter.ProvinceListAdapter
import com.rickon.ximalayakotlin.adapter.RadioListAdapter
import com.ximalaya.ting.android.opensdk.constants.DTransferConstants
import com.ximalaya.ting.android.opensdk.datatrasfer.CommonRequest
import com.ximalaya.ting.android.opensdk.datatrasfer.IDataCallBack
import com.ximalaya.ting.android.opensdk.model.PlayableModel
import com.ximalaya.ting.android.opensdk.model.live.provinces.Province
import com.ximalaya.ting.android.opensdk.model.live.provinces.ProvinceList
import com.ximalaya.ting.android.opensdk.model.live.radio.Radio
import com.ximalaya.ting.android.opensdk.model.live.radio.RadioList
import com.ximalaya.ting.android.opensdk.player.XmPlayerManager
import com.ximalaya.ting.android.opensdk.player.service.IXmPlayerStatusListener
import com.ximalaya.ting.android.opensdk.player.service.XmPlayerException
import kotlinx.android.synthetic.main.radio_frag_layout.*

/**
 * @Description: online FM
 * @Author:      高烁
 * @CreateDate:  2019-06-14 15:06
 * @Email:       gaoshuo521@foxmail.com
 */
class RadioFragment : BaseFragment() {
    private lateinit var mContext: Context
    private var mProvinceList: List<Province>? = null
    private var mRadioList: List<Radio>? = null

    private lateinit var radioAdapter:RadioListAdapter

    private var provinceCode: Long = 110000L
    private var mLoading = false
    private var currentRadioPos = Int.MAX_VALUE
    private var currentProvincePos = 0

    private var mPlayerServiceManager: XmPlayerManager? = null


    //uiHandler在主线程中创建，所以自动绑定主线程
    private var uiHandler = object : Handler() {
        override fun handleMessage(msg: Message?) {
            super.handleMessage(msg)
            when (msg?.what) {
                LOAD_PROVINCE_SUCCESS -> {

                    recyclerview_provinces.layoutManager = LinearLayoutManager(mContext)
                    val provinceAdapter = ProvinceListAdapter(mContext, mProvinceList!!)
                    recyclerview_provinces.adapter = provinceAdapter
                    //默认选中第一项
                    provinceAdapter.setSelectItem(currentProvincePos)

                    provinceAdapter.setOnKotlinItemClickListener(object :
                        ProvinceListAdapter.IKotlinItemClickListener {
                        override fun onItemClickListener(position: Int) {
                            if (position != currentProvincePos) {
                                Log.d(TAG, position.toString())
                                currentProvincePos = position

                                provinceAdapter.setSelectItem(position)
                                provinceAdapter.notifyDataSetChanged()

                                provinceCode = mProvinceList!![position].provinceCode
                                loadRadioList()
                            }
                        }
                    })
                }

                LOAD_RADIO_SUCCESS -> {
                    recyclerview_radios.layoutManager = LinearLayoutManager(mContext)
                    radioAdapter = RadioListAdapter(mContext, mRadioList!!)
                    recyclerview_radios.adapter = radioAdapter

                    radioAdapter.setOnKotlinItemClickListener(object : RadioListAdapter.IKotlinItemClickListener {
                        override fun onItemClickListener(position: Int) {
                            if (position != currentRadioPos) {
                                Log.d(TAG, position.toString())

                                val radio = mRadioList?.get(position)
                                //播放直播
                                mPlayerServiceManager?.playLiveRadioForSDK(radio, -1, -1)

                                currentRadioPos = position
                                radioAdapter.setSelectItem(position)
                                radioAdapter.notifyDataSetChanged()
                            }
                        }
                    })
                }
            }
        }

    }

    private val mPlayerStatusListener = object : IXmPlayerStatusListener {
        override fun onSoundSwitch(laModel: PlayableModel?, curModel: PlayableModel) {
            radioAdapter.notifyDataSetChanged()
        }

        override fun onSoundPrepared() {}

        override fun onSoundPlayComplete() {}

        override fun onPlayStop() {}

        override fun onPlayStart() {}

        override fun onPlayProgress(currPos: Int, duration: Int) {}

        override fun onPlayPause() {}

        override fun onError(exception: XmPlayerException): Boolean {
            return false

        }

        override fun onBufferingStop() {}

        override fun onBufferingStart() {}

        override fun onBufferProgress(percent: Int) {}

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.radio_frag_layout, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        mContext = activity!!

        mPlayerServiceManager = XmPlayerManager.getInstance(mContext)
        mPlayerServiceManager?.addPlayerStatusListener(mPlayerStatusListener)

        loadProvinceList()
        loadRadioList()

        edit_text_main.setOnClickListener { Toast.makeText(mContext,"跳转搜索",Toast.LENGTH_SHORT).show() }

    }

    override fun onDestroy() {
        super.onDestroy()
    }

    /**
     * 获取省份列表
     */
    private fun loadProvinceList() {
        val map = java.util.HashMap<String, String>()
        CommonRequest.getProvinces(map, object : IDataCallBack<ProvinceList> {
            override fun onSuccess(provinceList: ProvinceList?) {
                mProvinceList = provinceList!!.provinceList
                Log.d(TAG, "获取省市列表成功")
                val msg = Message()
                msg.what = LOAD_PROVINCE_SUCCESS
                uiHandler.sendMessage(msg)
            }

            override fun onError(i: Int, s: String) {
                Log.d(TAG, "获取省市列表失败")
            }
        })
    }

    /**
     * 加载对应省份直播电台不播放
     */
    fun loadRadioList() {
        Log.d(TAG, "加载对应省份直播电台不播放")
        if (mLoading) {
            return
        }
        mLoading = true
        val map = HashMap<String, String>()
        map[DTransferConstants.RADIOTYPE] = PROVINCE_RADIO_TYPE.toString()
        map[DTransferConstants.PROVINCECODE] = provinceCode.toString()
        CommonRequest.getRadios(map, object : IDataCallBack<RadioList> {
            override fun onSuccess(radioList: RadioList?) {
                if (radioList?.radios != null) {
                    mRadioList = null
                    mRadioList = radioList.radios

                    val msg = Message()
                    msg.what = LOAD_RADIO_SUCCESS
                    uiHandler.sendMessage(msg)
                }
                mLoading = false
            }

            override fun onError(code: Int, message: String) {
                mLoading = false
                Log.d(TAG, "获取省市下的电台失败,错误码$code,错误信息$message")
            }
        })
    }

    companion object {

        private const val TAG = "RadioFragment"
        private const val COUNTRY_RADIO_TYPE = 1
        private const val PROVINCE_RADIO_TYPE = 2
        private const val NET_RADIO_TYPE = 3
        private const val LOAD_PROVINCE_SUCCESS = 4
        private const val LOAD_RADIO_SUCCESS = LOAD_PROVINCE_SUCCESS + 1


    }
}