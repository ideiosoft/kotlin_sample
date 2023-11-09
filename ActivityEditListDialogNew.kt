package com.agricolum.ui.fragment


import com.agricolum.ui.fragment.BaseFragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import com.agricolum.ui.view.EnclosureFilterView
import com.agricolum.storage.api.AgricolumStore
import com.agricolum.ui.view.SharpButton
import com.agricolum.ui.activities.MainActivity
import com.agricolum.ui.view.SearchEditText
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.agricolum.domain.response.CropColorsModel
import com.agricolum.R
import com.agricolum.ui.fields.EnclosureListPresenter
import com.agricolum.ui.presenter.ActivityListPresenter
import com.agricolum.ui.view.ActivitiesFilterView
import android.os.Bundle
import com.agricolum.ui.presenter.ActivityListPickerPresenter
import com.agricolum.ui.util.SwipeRefreshLayoutManager
import android.text.TextWatcher
import com.agricolum.ui.fragment.ActivityEditListDialogNew
import android.text.Editable
import android.view.View.OnFocusChangeListener
import android.widget.TextView.OnEditorActionListener
import android.view.inputmethod.EditorInfo
import com.agricolum.ui.view.SearchEditText.KeyImeChange
import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Message
import android.util.Log
import android.util.SparseBooleanArray
import android.view.*
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.*
import com.agricolum.Util
import com.agricolum.domain.model.*
import com.agricolum.ui.contracts.ActivityListPickerContracts
import java.lang.Exception
import java.util.*
import java.util.regex.Pattern

/**
 * This dialog is used to select enclosures, fields, workers and machines
 * when creating an activity.
 * TODO: this screen is doing too much, it should be split in separate modals for each task.
 */
class ActivityEditListDialogNew() : BaseFragment(), ActivityListPickerContracts.View,
    OnRefreshListener, EnclosureFilterView.FilterViewListener {
    private val canClose = false
    private var mComplete = false
    private var mStore: AgricolumStore? = null
    private var treatments: List<PartialTreatment>? = null
    private var mLabelMessage: TextView? = null
    private var mLabelMessageEnclosures: TextView? = null
    private var mAddEnclosureButton: SharpButton? = null
    private var mButtonAccept: Button? = null
    private var mFilterView: EnclosureFilterView? = null
    private var presenter: ActivityListPickerContracts.Presenter? = null
    var _v: View? = null
    var mObjectsToShow: ArrayList<Any>? = null
    var mObjectsPreviouslySelected: ArrayList<Any>? = null
    var filteredEnclosures: ArrayList<Any>? = ArrayList()
    var filteredEnclosuresIterators: ArrayList<Int> = ArrayList<Int>()

    // Use this instance of the interface to deliver action events
    var mListener: NoticeListDialogListener? = null
    var type: PickerMode? = null
    private val mActivity: MainActivity? = null
    private var mFilterTxt: SearchEditText? = null
    private var mFilterTxtFrm: View? = null
    private var mListView: ListView? = null
    private var mSwipeRefresh: SwipeRefreshLayout? = null
    override fun onClickFilterView(
        startDate: Long,
        endDate: Long,
        explotationsChecked: List<Explotation>,
        activitiesChecked: List<String>,
        cropColorChecked: List<CropColorsModel>,
        useSigpacChecked: List<Enclosure>,
        checked: Boolean
    ) {
        mFilterView!!.animation = AnimationUtils.loadAnimation(context, R.anim.slide_down)
        mFilterView!!.visibility = View.GONE
        when (type) {
            PickerMode.MACHINES -> {}
            PickerMode.WORKERS -> {}
            PickerMode.ENCLOSURES -> {
                MainActivity.mEnclosuresChecked = ArrayList()
                MainActivity.mActivityExplotationChecked = ArrayList()
                MainActivity.mEnclosuresChecked.addAll(explotationsChecked)
                MainActivity.mActivityExplotationChecked.addAll(explotationsChecked)
                MainActivity.mCropsChecked = ArrayList()
                MainActivity.mColorCropsChecked = ArrayList()
                EnclosureListPresenter.mExplotationsChecked = ArrayList()
                EnclosureListPresenter.mExplotationsChecked!!.addAll(explotationsChecked)
                MainActivity.mCropsChecked.addAll(activitiesChecked)
                MainActivity.mColorCropsChecked.addAll(cropColorChecked)
                if (ActivityListPresenter.mExplotationsChecked.size == explotationsChecked.size && MainActivity.mCropsChecked.size == EnclosureFilterView.newColorList.size) {
                    val enclosures = mStore!!.getEnclosureList(
                        mStore!!.currentUserId
                    )
                    displayEnclosures(enclosures)
                } else {
                    val enclosures = filterActivities()
                    val enclosuresTemp: MutableList<Enclosure> = ArrayList()
                    val handler: Handler
                    handler = object : Handler() {
                        override fun handleMessage(msg: Message) {
                            activity!!.runOnUiThread(Runnable {
                                if (enclosuresTemp != null) {
                                    displayEnclosures(enclosuresTemp)
                                }
                            })
                        }
                    }
                    Thread(object : Runnable {
                        override fun run() {
                            if (MainActivity.mCropsChecked.size == ActivitiesFilterView.newColorListCrop.size || MainActivity.mCropsChecked.size == 0) {
                                enclosuresTemp.addAll((enclosures)!!)
                            } else {
                                if (MainActivity.tempProductList != null) {
                                    var k = 0
                                    while (k < MainActivity.tempProductList.size) {
                                        if (MainActivity.mCropsChecked != null) {
                                            var j = 0
                                            while (j < MainActivity.mCropsChecked.size) {
                                                if (MainActivity.mCropsChecked[j].contains("-")) {
                                                    if (MainActivity.mCropsChecked[j].split("-")
                                                            .toTypedArray().size > 1
                                                    ) {
                                                        if (MainActivity.mCropsChecked[j].split("-")
                                                                .toTypedArray()[1] != null
                                                        ) {
                                                            if (MainActivity.tempProductList[k].seedId.toString()
                                                                    .equals(
                                                                        MainActivity.mCropsChecked[j].split(
                                                                            "-"
                                                                        ).toTypedArray()[0],
                                                                        ignoreCase = true
                                                                    ) &&
                                                                MainActivity.tempProductList[k].variety.toString()
                                                                    .equals(
                                                                        MainActivity.mCropsChecked[j].split(
                                                                            "-"
                                                                        ).toTypedArray()[1],
                                                                        ignoreCase = true
                                                                    )
                                                            ) {
                                                                if (enclosures != null) {
                                                                    var i = 0
                                                                    while (i < enclosures.size) {
                                                                        if (MainActivity.tempProductList[k].enclosure_id.equals(
                                                                                enclosures[i].id,
                                                                                ignoreCase = true
                                                                            )
                                                                        ) {
                                                                            enclosuresTemp.add(
                                                                                enclosures[i]
                                                                            )
                                                                        }
                                                                        i++
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                                j++
                                            }
                                        }
                                        k++
                                    }
                                }
                            }
                            handler.handleMessage(Message())
                        }
                    }).start()
                }
            }
            PickerMode.FIELDS -> {}
        }
    }

    override fun onClickColor(id: String, s: String) {}
    override fun onCropClickColor(
        activityId: String,
        itemId: String,
        mode: String,
        color: String
    ) {
    }

    private fun explotationsCheckedContains(
        explotationsChecked: List<Explotation>,
        value: String
    ): Boolean {
        for (explotation: Explotation in explotationsChecked) {
            if ((explotation.id == value) || (explotation.name!!.split(",")
                    .toTypedArray()[0] == value)
            ) {
                return true
            }
        }
        return false
    }

    private fun filterActivities(): List<Enclosure>? {
        val enclosures = mStore!!.getEnclosureList(mStore!!.currentUserId)
        val enclosuresTemp: MutableList<Enclosure> = ArrayList()
        if (enclosures != null) {
            for (activity: Enclosure in enclosures) {
                if (activity.explotation_id != null) {
                    if (MainActivity.mActivityExplotationChecked != null) {
                        if (explotationsCheckedContains(
                                MainActivity.mActivityExplotationChecked,
                                activity.explotation_id
                            )
                        ) {
                            enclosuresTemp.add(activity)
                        }
                    }
                }
            }
        }
        // Log.e("sdsds",new Gson().toJson(mStore.getEnclosure(enclosures.get(0).id)));
        if ((MainActivity.mEnclosuresChecked.size == MainActivity.mActivityExplotationChecked.size) && (MainActivity.mCropsChecked.size == MainActivity.tempProductList.size) && (MainActivity.selectedUseSigpacFilter.size == MainActivity.mUseSigpacFilter.size)) {
            return enclosures
        } else {
            val enclosuresTempp = ArrayList<Enclosure>()
            if (MainActivity.selectedUseSigpacFilter.size > 0) {
                for (i in enclosuresTemp.indices) {
                    for (j in MainActivity.selectedUseSigpacFilter.indices) {
                        val enclosure = mStore!!.getEnclosure(enclosuresTemp[i].id)
                        if (MainActivity.selectedUseSigpacFilter[j].use.equals(
                                enclosure.use,
                                ignoreCase = true
                            )
                        ) {
                            enclosuresTempp.add(enclosure)
                        }
                    }
                }
            } else {
                enclosuresTempp.clear()
                enclosuresTempp.addAll(enclosuresTemp)
            }
            return enclosuresTempp
        }
    }

    enum class PickerMode {
        ENCLOSURES, FIELDS, WORKERS, MACHINES
    }

    /*
     * The activity that creates an instance of this dialog fragment must
     * implement this interface in order to receive event callbacks. Each method
     * passes the DialogFragment in case the host needs to query it.
     */
    interface NoticeListDialogListener {
        fun onSelectOkDialogClick()
    }

    fun setListener(listener: NoticeListDialogListener?) {
        mListener = listener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_bales_transports, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_bales_filter -> {
                showHideFilter()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    private fun showHideFilter() {
        if (mFilterView!!.visibility == View.VISIBLE) {
            mFilterView!!.animation = AnimationUtils.loadAnimation(context, R.anim.slide_down)
            mFilterView!!.visibility = View.GONE
        } else {
            val enclosuress = mStore!!.getEnclosureList(
                mStore!!.currentUserId
            )
            mFilterView!!.setUseSigpac(activity, enclosuress)
            mFilterView!!.animation = AnimationUtils.loadAnimation(context, R.anim.slide_up)
            mFilterView!!.visibility = View.VISIBLE
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _v = inflater.inflate(R.layout.dialog_activityedit_list, container, false)
        presenter = ActivityListPickerPresenter(context, this)
        mStore = AgricolumStore.client(activity)
        if (treatments == null) {
            treatments = ArrayList()
        }

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        if (type != null) {
            when (type) {
                PickerMode.ENCLOSURES, PickerMode.FIELDS -> _v =
                    requireActivity().layoutInflater.inflate(R.layout.dialog_activity_edit_fields, null)
                else -> _v =
                    requireActivity().layoutInflater.inflate(R.layout.dialog_activityedit_list, null)
            }
        } else {
            _v = requireActivity().layoutInflater.inflate(R.layout.dialog_activityedit_list, null)
        }
        mFilterView = _v?.findViewById(R.id.filter_viewww)
        mFilterView?.setListener(this)
        MainActivity.mActivityExplotationCheckedTemp.clear()
        MainActivity.mActivityExplotationCheckedTemp.addAll(MainActivity.mEnclosuresChecked)
        mFilterView?.setExplotations(
            activity,
            ActivityListPresenter.mExplotationsChecked,
            MainActivity.mEnclosuresChecked
        )
        mFilterView?.setActivities(activity, MainActivity.tempProductList)
        val activityLay = mFilterView?.findViewById<LinearLayout>(R.id.activityLay)
        val cropsLay = mFilterView?.findViewById<LinearLayout>(R.id.cropsLay)
        val sigpacLay = mFilterView?.findViewById<LinearLayout>(R.id.sigpacLay)
        sigpacLay?.visibility = View.VISIBLE
        cropsLay?.visibility = View.GONE
        val filterTitle = mFilterView?.findViewById<TextView>(R.id.filterTitle)
        filterTitle?.text = resources.getString(R.string.filter_by_crop)
        activityLay?.visibility = View.VISIBLE
        mFilterTxt = _v?.findViewById(R.id.dlg_activityEdit_filter)
        mFilterTxtFrm = _v?.findViewById(R.id.dlg_activityEdit_filterFrm)
        mLabelMessage = _v?.findViewById(R.id.activityEditListDialogMessage)
        mLabelMessageEnclosures = _v?.findViewById(R.id.activityEditListDialog_no_enclosures_msg)
        mAddEnclosureButton = _v?.findViewById(R.id.activityEdit_list_addEnclosure)
        mButtonAccept = _v?.findViewById(R.id.dlg_activityEdit_okBtn)
        mListView = _v?.findViewById(android.R.id.list)
        mSwipeRefresh = _v?.findViewById(R.id.swipe_container)
        mSwipeRefresh?.setOnRefreshListener(this)
        SwipeRefreshLayoutManager.setup(mSwipeRefresh)
        mFilterTxt?.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                // TODO Auto-generated method stub
                Log.d(Companion.TAG, "onTextChanged '$s'")
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                // TODO Auto-generated method stub
            }

            override fun afterTextChanged(s: Editable) {
                //  refreshList();
            }
        })
        mFilterTxt?.setOnFocusChangeListener(object : OnFocusChangeListener {
            override fun onFocusChange(v: View, hasFocus: Boolean) {
                if (hasFocus) {
                    if (mAddEnclosureButton != null) {
                        mAddEnclosureButton!!.visibility = View.GONE
                        mButtonAccept?.setVisibility(View.GONE)
                        _v?.findViewById<View>(R.id.dlg_activityEdit_selectAllFrm)?.visibility =
                            View.GONE
                    }
                } else {
                    // dismissKeyboard(_v);
                    if (mAddEnclosureButton != null) {
                        mAddEnclosureButton!!.visibility = View.VISIBLE
                        mButtonAccept?.setVisibility(View.VISIBLE)
                        _v?.findViewById<View>(R.id.dlg_activityEdit_selectAllFrm)!!.visibility =
                            View.VISIBLE
                    }
                }
            }
        })
        mFilterTxt?.setOnEditorActionListener(object : OnEditorActionListener {
            override fun onEditorAction(
                textView: TextView,
                actionId: Int,
                keyEvent: KeyEvent
            ): Boolean {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    // dismiss keyboard on search action
                    refreshList()
                    // mFilterTxt.clearFocus();
                    val `in` =
                        activity!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    `in`.hideSoftInputFromWindow(mFilterTxt?.getWindowToken(), 0)
                    return true
                }
                return false
            }
        })
        mFilterTxt?.setKeyImeChangeListener(object : KeyImeChange {
            override fun onKeyIme(keyCode: Int, event: KeyEvent) {
                if (KeyEvent.KEYCODE_BACK == event.keyCode) {
                    mAddEnclosureButton?.setVisibility(View.VISIBLE)
                    mButtonAccept?.setVisibility(View.VISIBLE)
                    _v?.findViewById<View>(R.id.dlg_activityEdit_selectAllFrm)!!.visibility =
                        View.VISIBLE
                }
            }
        })
        var title: String? = null
        when (type) {
            PickerMode.MACHINES -> title = getString(R.string.activityEdit_tractorsTxt)
            PickerMode.WORKERS -> title = getString(R.string.activityEdit_peopleTxt)
            PickerMode.ENCLOSURES -> {
                title = getString(R.string.activityEdit_enclosureTxt)
                mFilterTxtFrm?.setVisibility(View.VISIBLE)
                _v?.findViewById<View>(R.id.dlg_activityEdit_selectAllFrm)!!.visibility = View.VISIBLE
                val mCheckToggleAll =
                    _v?.findViewById<View>(R.id.activityEditListDialog_toggleAll) as CheckBox
                mCheckToggleAll.setOnClickListener(object : View.OnClickListener {
                    override fun onClick(v: View) {
                        toggleListChecks(mCheckToggleAll.isChecked)
                    }
                })
                mAddEnclosureButton?.setVisibility(View.VISIBLE)
                mAddEnclosureButton?.setOnClickListener(object : View.OnClickListener {
                    override fun onClick(v: View) {
                        mActivity.showEnclosureMap(null, null, 3)
                    }
                })
                mAddEnclosureButton?.setText(R.string.create_enclosure)
            }
            PickerMode.FIELDS -> {
                title = getString(R.string.fields)
                mFilterTxt?.setHint(R.string.field_search)
                mFilterTxtFrm?.setVisibility(View.VISIBLE)
                _v?.findViewById<View>(R.id.dlg_activityEdit_selectAllFrm)?.visibility = View.VISIBLE
                mAddEnclosureButton?.setVisibility(View.VISIBLE)
                mAddEnclosureButton?.setOnClickListener(object : View.OnClickListener {
                    override fun onClick(v: View) {
                        mActivity.showFieldMap(null, 3)
                    }
                })
                mAddEnclosureButton?.setText(R.string.create_field)
                val mCheckToggleAll =
                    _v?.findViewById<View>(R.id.activityEditListDialog_toggleAll) as CheckBox
                mCheckToggleAll.setOnClickListener(object : View.OnClickListener {
                    override fun onClick(v: View) {
                        toggleListChecks(mCheckToggleAll.isChecked)
                    }
                })
            }
        }
        (_v?.findViewById<View>(R.id.dlg_activityEdit_title) as TextView).text = title
        val lv = _v?.findViewById<ListView>(android.R.id.list)
        lv?.choiceMode = ListView.CHOICE_MODE_MULTIPLE
        mButtonAccept?.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View) {
                mListener!!.onSelectOkDialogClick()
            }
        })
        (_v?.findViewById<View>(R.id.dummy) as TextView).onFocusChangeListener = object : OnFocusChangeListener {
            override fun onFocusChange(v: View, hasFocus: Boolean) {
                if (hasFocus && canClose) {
                    mListener!!.onSelectOkDialogClick()
                }
            }
        }
        return _v
    }

    private fun toggleListChecks(checked: Boolean) {
        if (mObjectsToShow == null || mObjectsToShow!!.isEmpty()) {
            return
        }
        if (type == PickerMode.ENCLOSURES || type == PickerMode.FIELDS) {
            if (mObjectsToShow != null) {
                for (i in mObjectsToShow!!.indices) {
                    val en = mObjectsToShow!![i] as Zone?
                    en!!.selected = checked
                }
            }
            refreshList()
        }
    }

    override fun onAttach(activity: Activity) {
        super.onAttach(activity)
        if (activity is MainActivity) {
            mActivity = activity
        }
    }

    override fun onStart() {
        super.onStart()
        if (presenter == null) {
            presenter = ActivityListPickerPresenter(context, this)
        }
    }

    override fun onStop() {
        super.onStop()
        presenter!!.cancelTasks()
        presenter = null
    }

    override fun onResume() {
        super.onResume()
        if (type == PickerMode.ENCLOSURES && mObjectsPreviouslySelected != null) {
            // enclosures already provided
            mObjectsToShow = mObjectsPreviouslySelected

            // sort by: checked first
            Collections.sort(mObjectsToShow, object : Comparator<Any?> {
                override fun compare(o1: Any?, o2: Any?): Int {
                    if (o1 is Zone && o2 is Zone) {
                        val e1 = o1
                        val e2 = o2
                        if (e1.selected && !e2.selected) {
                            return -1
                        }
                        if (!e1.selected && e2.selected) {
                            return 1
                        }
                        return if (e1.name == null || e2.name == null) {
                            0
                        } else e1.name.compareTo(e2.name)
                    }
                    return 0
                }
            })
            applyTextFilter()
            //checkEnclosures(mObjectsToShow);
            refreshList()
            //showData();
            return
        }
        if (type == PickerMode.FIELDS && mObjectsPreviouslySelected != null) {
            // fields already provided
            mObjectsToShow = mObjectsPreviouslySelected
            applyTextFilter()
            refreshList()
            return
        }

        // fetch data otherwise
        update()
    }

    override fun onDestroy() {
        super.onDestroy()
        mActivity.showingDialog = false
    }

    override fun onDetach() {
        super.onDetach()
        mActivity = null
    }

    /**
     * Called when table view is set to refresh (swipe control).
     */
    override fun onRefresh() {
        if (mObjectsPreviouslySelected != null && !mObjectsPreviouslySelected!!.isEmpty()) {
            // if data is constrained, do not fetch new data
            mSwipeRefresh!!.isRefreshing = false
            return
        }
        when (type) {
            PickerMode.MACHINES -> presenter!!.fetchMachines(true)
            PickerMode.WORKERS -> presenter!!.fetchWorkers(true)
            PickerMode.ENCLOSURES -> presenter!!.fetchEnclosures(true)
            PickerMode.FIELDS -> presenter!!.fetchFields(true)
        }
    }

    private fun update() {
        if (presenter == null) {
            return
        }
        when (type) {
            PickerMode.MACHINES -> presenter!!.fetchMachines(false)
            PickerMode.WORKERS -> presenter!!.fetchWorkers(false)
            PickerMode.ENCLOSURES -> presenter!!.fetchEnclosures(false)
            PickerMode.FIELDS -> presenter!!.fetchFields(false)
        }
    }

    private fun checkEnclosures(list: List<Any?>?) {
        if (list != null) {
            for (obj: Any? in list) {
                val zone = obj as Zone?
                if (treatments != null) {
                    // check if there is a partial treatment for the zone
                    for (treatment: PartialTreatment in treatments!!) {
                        if ((treatment.zone == zone)) {
                            zone!!.selected = true
                            zone.treatment = true
                            zone.setPath(treatment.path)
                        }
                    }
                }
            }
        }
    }

    fun setPartialTreatments(treatments: List<PartialTreatment>?) {
        this.treatments = treatments
        update()
    }

    override fun startLoading() {
        mSwipeRefresh!!.isRefreshing = true
    }

    override fun stopLoading() {
        mSwipeRefresh!!.isRefreshing = false
    }

    fun refreshList() {
        //showData();
        val adapter: ObjectListAdapter? = mListView!!.adapter as ObjectListAdapter
        adapter?.notifyDataSetChanged()
    }

    private fun applyTextFilter() {
        filteredEnclosures = ArrayList()
        filteredEnclosuresIterators = ArrayList<Int>()
        if (mObjectsToShow != null) {
            for (i in mObjectsToShow!!.indices) {
                val en = mObjectsToShow!![i] as Zone?
                // check text filter
                val filter = mFilterTxt!!.text.toString()
                var matched = false
                if (en!!.name != null) {
                    matched = Pattern.compile(Pattern.quote(filter), Pattern.CASE_INSENSITIVE)
                        .matcher(en.name)
                        .find()
                } else if (en.reference != null) {
                    matched = Pattern.compile(Pattern.quote(filter), Pattern.CASE_INSENSITIVE)
                        .matcher(en.reference)
                        .find()
                }
                if (matched) {
                    filteredEnclosuresIterators.add(i)
                    filteredEnclosures!!.add(en)
                }
            }
        }
        checkEnclosures(filteredEnclosures)
        mObjectsToShow = ArrayList()
        if (filteredEnclosures != null) {
            mObjectsToShow!!.addAll(filteredEnclosures!!)
        }
        if (mObjectsToShow != null) {
            mListView!!.adapter = ObjectListAdapter(
                mActivity,
                0,
                mObjectsToShow,
                requireActivity().layoutInflater
            )
        }
    }

    override fun displayEnclosures(enclosures: List<Enclosure>?) {
        if (mActivity == null) {
            return
        }
        if (MainActivity.mActivityExplotationChecked.size == 0 && MainActivity.mCropsChecked.size == 0) {
            mObjectsToShow = ArrayList()
            if (enclosures != null) {
                for (enclosure: Enclosure? in enclosures) {
                    val zone = Util.getZoneFrom(enclosure)
                    mObjectsToShow!!.add(zone)
                    // check if there is a partial treatment for the zone
                    if (treatments != null) {
                        for (treatment: PartialTreatment in treatments!!) {
                            if ((treatment.zone == zone)) {
                                zone.selected = true
                                zone.treatment = true
                                zone.setPath(treatment.path)
                            }
                        }
                    }
                }
            }

            // sort by: checked first
            Collections.sort(mObjectsToShow, object : Comparator<Any?> {
                override fun compare(o1: Any?, o2: Any?): Int {
                    if (o1 is Zone && o2 is Zone) {
                        val e1 = o1
                        val e2 = o2
                        if (e1.selected && !e2.selected) {
                            return -1
                        }
                        return if (!e1.selected && e2.selected) {
                            1
                        } else e1.name.compareTo(e2.name)
                    }
                    return 0
                }
            })
        } else if (ActivityListPresenter.mExplotationsChecked.size == MainActivity.mActivityExplotationChecked.size && MainActivity.mCropsChecked.size == 0) {
            mObjectsToShow = ArrayList()
            if (enclosures != null) {
                for (enclosure: Enclosure? in enclosures) {
                    val zone = Util.getZoneFrom(enclosure)
                    mObjectsToShow!!.add(zone)
                    // check if there is a partial treatment for the zone
                    if (treatments != null) {
                        for (treatment: PartialTreatment in treatments!!) {
                            if ((treatment.zone == zone)) {
                                zone.selected = true
                                zone.treatment = true
                                zone.setPath(treatment.path)
                            }
                        }
                    }
                }
            }

            // sort by: checked first
            Collections.sort(mObjectsToShow, object : Comparator<Any?> {
                override fun compare(o1: Any?, o2: Any?): Int {
                    if (o1 is Zone && o2 is Zone) {
                        val e1 = o1
                        val e2 = o2
                        if (e1.selected && !e2.selected) {
                            return -1
                        }
                        return if (!e1.selected && e2.selected) {
                            1
                        } else e1.name.compareTo(e2.name)
                    }
                    return 0
                }
            })
        } else if (ActivityListPresenter.mExplotationsChecked.size == MainActivity.mActivityExplotationChecked.size && MainActivity.mCropsChecked.size == EnclosureFilterView.newColorList.size) {
            mObjectsToShow = ArrayList()
            if (enclosures != null) {
                for (enclosure: Enclosure? in enclosures) {
                    val zone = Util.getZoneFrom(enclosure)
                    mObjectsToShow!!.add(zone)
                    // check if there is a partial treatment for the zone
                    if (treatments != null) {
                        for (treatment: PartialTreatment in treatments!!) {
                            if ((treatment.zone == zone)) {
                                zone.selected = true
                                zone.treatment = true
                                zone.setPath(treatment.path)
                            }
                        }
                    }
                }
            }

            // sort by: checked first
            Collections.sort(mObjectsToShow, object : Comparator<Any?> {
                override fun compare(o1: Any?, o2: Any?): Int {
                    if (o1 is Zone && o2 is Zone) {
                        val e1 = o1
                        val e2 = o2
                        if (e1.selected && !e2.selected) {
                            return -1
                        }
                        return if (!e1.selected && e2.selected) {
                            1
                        } else e1.name.compareTo(e2.name)
                    }
                    return 0
                }
            })
        } else if (ActivityListPresenter.mExplotationsChecked.size != MainActivity.mActivityExplotationChecked.size && MainActivity.mCropsChecked.size == EnclosureFilterView.newColorList.size) {
            val enclosuresss = filterActivities()
            mObjectsToShow = ArrayList()
            val enclosuresTemp: MutableList<Enclosure> = ArrayList()
            enclosuresTemp.addAll((enclosuresss)!!)
            if (enclosuresTemp != null) {
                for (enclosure: Enclosure? in enclosuresTemp) {
                    val zone = Util.getZoneFrom(enclosure)
                    mObjectsToShow!!.add(zone)
                    // check if there is a partial treatment for the zone
                    if (treatments != null) {
                        for (treatment: PartialTreatment in treatments!!) {
                            if ((treatment.zone == zone)) {
                                zone.selected = true
                                zone.treatment = true
                                zone.setPath(treatment.path)
                            }
                        }
                    }
                }
            }

            // sort by: checked first
            Collections.sort(mObjectsToShow, object : Comparator<Any?> {
                override fun compare(o1: Any?, o2: Any?): Int {
                    if (o1 is Zone && o2 is Zone) {
                        val e1 = o1
                        val e2 = o2
                        if (e1.selected && !e2.selected) {
                            return -1
                        }
                        return if (!e1.selected && e2.selected) {
                            1
                        } else e1.name.compareTo(e2.name)
                    }
                    return 0
                }
            })
        } else {
            val enclosuresss = filterActivities()
            val enclosuresTemp: MutableList<Enclosure> = ArrayList()
            if ((MainActivity.tempProductList != null) && (MainActivity.tempProductList.size > 0) && (MainActivity.mCropsChecked != null) && (MainActivity.mCropsChecked.size > 0)) {
                for (k in MainActivity.tempProductList.indices) {
                    if (MainActivity.mCropsChecked != null) {
                        for (j in MainActivity.mCropsChecked.indices) {
                            if (MainActivity.mCropsChecked[j].contains("-")) {
                                if (MainActivity.mCropsChecked[j].split("-")
                                        .toTypedArray().size > 1
                                ) {
                                    if (MainActivity.mCropsChecked[j].split("-")
                                            .toTypedArray()[1] != null
                                    ) {
                                        if (MainActivity.tempProductList[k].seedId.toString()
                                                .equals(
                                                    MainActivity.mCropsChecked[j].split("-")
                                                        .toTypedArray()[0], ignoreCase = true
                                                ) &&
                                            MainActivity.tempProductList[k].variety.toString()
                                                .equals(
                                                    MainActivity.mCropsChecked[j].split("-")
                                                        .toTypedArray()[1], ignoreCase = true
                                                )
                                        ) {
                                            if (enclosuresss != null) {
                                                for (i in enclosuresss.indices) {
                                                    if (MainActivity.tempProductList[k].enclosure_id.equals(
                                                            enclosuresss[i].id, ignoreCase = true
                                                        )
                                                    ) {
                                                        enclosuresTemp.add(enclosuresss[i])
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                enclosuresTemp.addAll((enclosuresss)!!)
            }
            // }
            mObjectsToShow = ArrayList()
            if (enclosuresTemp != null) {
                for (enclosure: Enclosure? in enclosuresTemp) {
                    val zone = Util.getZoneFrom(enclosure)
                    mObjectsToShow!!.add(zone)
                    // check if there is a partial treatment for the zone
                    if (treatments != null) {
                        for (treatment: PartialTreatment in treatments!!) {
                            if ((treatment.zone == zone)) {
                                zone.selected = true
                                zone.treatment = true
                                zone.setPath(treatment.path)
                            }
                        }
                    }
                }
            }

            // sort by: checked first
            Collections.sort(mObjectsToShow, object : Comparator<Any?> {
                override fun compare(o1: Any?, o2: Any?): Int {
                    if (o1 is Zone && o2 is Zone) {
                        val e1 = o1
                        val e2 = o2
                        if (e1.selected && !e2.selected) {
                            return -1
                        }
                        return if (!e1.selected && e2.selected) {
                            1
                        } else e1.name.compareTo(e2.name)
                    }
                    return 0
                }
            })
        }
        if (enclosures!!.size == 0 && _v != null) {
            // display 'no enclosures' message
            mLabelMessageEnclosures!!.visibility = View.VISIBLE
            mLabelMessageEnclosures!!.setText(R.string.no_enclosures)
        } else {
            mLabelMessageEnclosures!!.visibility = View.GONE
        }
        applyTextFilter()
        markSelectedItems()
    }

    override fun appendEnclosures(enclosures: List<Enclosure>?) {
        if (mActivity == null) {
            return
        }
        if (enclosures != null) {
            for (enclosure: Enclosure in enclosures) {
                mObjectsToShow!!.add(enclosure.getZone())
            }
        }
        applyTextFilter()
        markSelectedItems()
    }

    override fun displayFields(fields: List<Field>?) {
        if (mActivity == null) {
            return
        }
        mObjectsToShow = ArrayList()
        if (fields != null) {
            for (field: Field in fields) {
                mObjectsToShow!!.add(field.zone)
            }
        }
        if (fields!!.size == 0 && _v != null) {
            // display 'no enclosures' message
            mLabelMessageEnclosures!!.visibility = View.VISIBLE
            mLabelMessageEnclosures!!.setText(R.string.no_fields)
        } else {
            mLabelMessageEnclosures!!.visibility = View.GONE
        }
        applyTextFilter()
        markSelectedItems()
    }

    override fun displayWorkers(workers: List<Worker>?) {
        if (mActivity == null) {
            return
        }
        mObjectsToShow = ArrayList()
        if (workers != null) {
            mObjectsToShow!!.addAll(workers)
        }
        mListView!!.adapter = ObjectListAdapter(
            mActivity,
            0,
            mObjectsToShow,
            requireActivity().layoutInflater
        )
        markSelectedItems()
        if (workers!!.size == 1 && _v != null) {
            // display 'no workers' message
            mLabelMessage!!.visibility = View.VISIBLE
            mLabelMessage!!.setText(R.string.no_workers)
        }
    }

    override fun displayMachines(machines: List<Machine>?) {
        if (mActivity == null) {
            return
        }
        mObjectsToShow = ArrayList()
        if (machines != null) {
            mObjectsToShow!!.addAll(machines)
        }
        mListView!!.adapter = ObjectListAdapter(
            mActivity,
            0,
            mObjectsToShow,
            requireActivity().layoutInflater
        )
        markSelectedItems()
        if (machines!!.size == 1 && _v != null) {
            // display 'no machines' message
            mLabelMessage!!.visibility = View.VISIBLE
            mLabelMessage!!.setText(R.string.no_machines)
        }
    }

    fun showError(error: Int) {
        if (mActivity == null) {
            return
        }
        mActivity.showError(error)
    }

    protected fun createPartialTreatment(enclosure: Enclosure?, zone: Zone?) {
        if (enclosure != null && zone != null) {
            mActivity.createPartialTreatment(treatments, enclosure, zone)
        }
    }

    protected fun createPartialTreatment(field: Field?, zone: Zone?) {
        if (field != null && zone != null) {
            mActivity.createPartialTreatment(treatments, field, zone)
        }
    }

    private inner class ObjectListAdapter(
        context: Context?,
        textViewResourceId: Int,
        objects: List<Any>?,
        inflater: LayoutInflater?
    ) : ArrayAdapter<Any?>(
        (context)!!, textViewResourceId, (objects)!!
    ) {
        private var mInflater: LayoutInflater? = null
        private var mEntries: List<Any>? = null

        init {
            mInflater = inflater
            mEntries = objects
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            var row = convertView
            val ae = mEntries!![position]

            // if null create it
            if (row == null || row.tag == null) {
                val holder = ViewHolder()
                when (type) {
                    PickerMode.MACHINES -> {
                        // get line view
                        row = mInflater!!.inflate(
                            R.layout.row_activity_editlist_machine,
                            parent,
                            false
                        )
                        holder.title = row.findViewById(R.id.activityEdit_listMachine_name)
                        holder.image = row.findViewById(R.id.activityEdit_listMachine_image)
                    }
                    PickerMode.WORKERS -> {
                        // get line view
                        row = mInflater!!.inflate(
                            R.layout.row_activity_editlist_people,
                            parent,
                            false
                        )
                        holder.title = row.findViewById(R.id.activityEdit_listWorker_name)
                        holder.image = row.findViewById(R.id.activityEdit_listWorker_image)
                    }
                    PickerMode.ENCLOSURES -> {
                        // get line view
                        row = mInflater!!.inflate(
                            R.layout.row_activity_editlist_enclosure,
                            parent,
                            false
                        )
                        holder.view = row
                        holder.title = row.findViewById(R.id.activityEdit_listEnclosure_name)
                        holder.image = row.findViewById(R.id.activityEdit_listEnclosure_image)
                        holder.back = row.findViewById(R.id.activityEdit_listEnclosure_bg)
                        holder.editText = row.findViewById(R.id.activityEdit_listEnclosure_area)
                        holder.check = row.findViewById(R.id.activityEdit_listEnclosure_select)
                        holder.partialButton = row.findViewById(R.id.activityEdit_btnPartial)
                    }
                    PickerMode.FIELDS -> {
                        // get line view
                        row = mInflater!!.inflate(
                            R.layout.row_activity_editlist_enclosure,
                            parent,
                            false
                        )
                        holder.view = row
                        holder.title = row.findViewById(R.id.activityEdit_listEnclosure_name)
                        holder.image = row.findViewById(R.id.activityEdit_listEnclosure_image)
                        holder.back = row.findViewById(R.id.activityEdit_listEnclosure_bg)
                        holder.editText = row.findViewById(R.id.activityEdit_listEnclosure_area)
                        holder.check = row.findViewById(R.id.activityEdit_listEnclosure_select)
                        holder.partialButton = row.findViewById(R.id.activityEdit_btnPartial)
                    }
                    else -> {}
                }
                row!!.tag = holder
            }

            // fill data
            val holder = row!!.tag as ViewHolder
            when (type) {
                PickerMode.MACHINES -> {
                    val ma = ae as Machine
                    holder.title!!.text = ma.name
                    if (ma.image64 != null) {
                        holder.image!!.setImageBitmap(ma.getImage())
                    }
                }
                PickerMode.WORKERS -> {
                    val wo = ae as Worker
                    holder.title!!.text = wo.full_name
                    if (wo.image64 != null) {
                        try {
                            holder.image!!.setImageBitmap(wo.getImage())
                        } catch (e: Exception) {
                            holder.image!!.setImageResource(R.drawable.ic_no_avatar)
                        }
                    }
                }
                else -> {
                    val en = ae as Zone
                    val filter = mFilterTxt!!.text.toString()
                    var matched = false
                    if (en.name != null) {
                        matched = Pattern.compile(Pattern.quote(filter), Pattern.CASE_INSENSITIVE)
                            .matcher(en.name)
                            .find()
                    } else if (en.reference != null) {
                        matched = Pattern.compile(Pattern.quote(filter), Pattern.CASE_INSENSITIVE)
                            .matcher(en.reference)
                            .find()
                    }
                    if (matched) {
                        holder.title!!.text = en.name
                        holder.editText!!.setText(en.area)
                        holder.editText!!.tag = en.area
                        holder.ref = (filteredEnclosuresIterators[position])!!
                        holder.setEnabled(en.enabled)
                        if (en.enabled) {
                            holder.check!!.isChecked = en.selected
                            holder.back!!.setBackgroundColor(if (en.selected) resources.getColor(R.color.app_background) else Color.WHITE)
                            holder.check!!.setOnCheckedChangeListener(object :
                                CompoundButton.OnCheckedChangeListener {
                                override fun onCheckedChanged(
                                    buttonView: CompoundButton,
                                    isChecked: Boolean
                                ) {
                                    (mObjectsToShow!![holder.ref] as Zone?)!!.selected = isChecked
                                    (buttonView.parent as View)
                                        .setBackgroundColor(
                                            if (isChecked) resources
                                                .getColor(R.color.app_background) else Color.WHITE
                                        )
                                }
                            })
                            holder.editText!!.onFocusChangeListener =
                                object : OnFocusChangeListener {
                                    override fun onFocusChange(
                                        v: View,
                                        hasFocus: Boolean
                                    ) {
                                        if (!hasFocus) {
                                            val area: String = v.tag as String
                                            var darea: Double = 0.0
                                            try {
                                                darea = area.toDouble()
                                            } catch (e: Exception) {
                                                darea = 0.0
                                            }
                                            var dareaWorked: Double = 0.0
                                            val areaWorked: String =
                                                (v as EditText).editableText.toString()
                                            try {
                                                dareaWorked = areaWorked.toDouble()
                                            } catch (e: Exception) {
                                                dareaWorked = 0.0
                                            }

                                            // if no area, return
                                            if (dareaWorked <= 0) {
                                                return
                                            }

                                            // if greater remove the data
                                            if (dareaWorked > darea) {
                                                Util.showToast(
                                                    activity,
                                                    R.string.general_error_greater_data
                                                )
                                                v.setText(v.getTag() as String?)
                                            } else {
                                                (mObjectsToShow!!.get(holder.ref) as Zone?)!!.area =
                                                    areaWorked
                                            }
                                        }
                                    }
                                }
                            holder.partialButton!!.setOnClickListener(object :
                                View.OnClickListener {
                                override fun onClick(v: View) {
                                    //Enclosure enclosure = mEnclosures.get(position);
                                    val zone = mObjectsToShow!![position] as Zone?
                                    if (zone!!.isEnclosure) {
                                        val enclosure = mStore!!.getEnclosure(zone.zoneable_id)
                                        enclosure?.let { createPartialTreatment(it, zone) }
                                            ?: Log.w(
                                                Companion.TAG,
                                                "Invalid Enclosure: " + zone.zoneable_id
                                            )
                                    } else if (zone.isField) {
                                        val field = mStore!!.getField(
                                            zone.zoneable_id
                                        )
                                        field?.let { createPartialTreatment(it, zone) }
                                            ?: Log.w(
                                                Companion.TAG,
                                                "Invalid Field: " + zone.zoneable_id
                                            )
                                    }
                                }
                            })
                        } else {
                            // display disabled (usually, when completing a task and the enclosures has already been used.)
                            holder.check!!.isChecked = false
                            holder.back!!.setBackgroundColor(resources.getColor(R.color.item_disabled_color))
                        }
                    } else {
                        return mInflater!!.inflate(R.layout.row_empty, parent, false)
                    }
                }
            }
            return (row)
        }
    }

    internal class ViewHolder() {
        var view: View? = null
        var title: TextView? = null
        var detail: TextView? = null
        var image: ImageView? = null
        var editText: EditText? = null
        var partialButton: Button? = null
        var check: CheckBox? = null
        var back: View? = null
        var ref = 0
        fun setEnabled(enabled: Boolean) {
            view!!.isEnabled = enabled
            check!!.isEnabled = enabled
            editText!!.isEnabled = enabled
            partialButton!!.isEnabled = enabled
        }
    }

    fun setSelectedItems(previouslySelectedObjects: ArrayList<*>?) {
        mObjectsPreviouslySelected = previouslySelectedObjects as ArrayList<Any>?
        if (mComplete && (type == PickerMode.ENCLOSURES || type == PickerMode.FIELDS)) {
            mObjectsToShow = mObjectsPreviouslySelected
        }
    }

    protected fun markSelectedItems() {
        if (mObjectsPreviouslySelected == null) {
            return
        }
        if (type == PickerMode.ENCLOSURES || type == PickerMode.FIELDS) {
            for (positionSelected in mObjectsPreviouslySelected!!.indices) {
                val eSelected = mObjectsPreviouslySelected!![positionSelected] as Zone?
                for (position in mObjectsToShow!!.indices) {
                    val e = mObjectsToShow!![position] as Zone?
                    try {
                        if ((eSelected!!.zoneable_id == e!!.zoneable_id)) {
                            e.id = eSelected.id
                            e.area = eSelected.area
                            e.selected = eSelected.selected
                            break
                        }
                    } catch (ec: Exception) {
                        /* avoid nulls in zoneable_id */
                    }
                }
                checkEnclosures(mObjectsToShow)
            }
        } else {
            val lv = _v!!.findViewById<ListView>(android.R.id.list)
            for (positionSelected in mObjectsPreviouslySelected!!
                .indices) {
                val objSelected = mObjectsPreviouslySelected!![positionSelected]
                var workSelected: Worker?
                var machSelected: Machine
                for (position in 0 until lv.count) {
                    if (objSelected is Machine) {
                        machSelected = objSelected
                        val machine = lv.getItemAtPosition(position) as Machine
                        if ((machine.id == machSelected.id)) {
                            lv.setItemChecked(position, true)
                            break
                        }
                    } else {
                        workSelected = objSelected as Worker?
                        val worker = lv.getItemAtPosition(position) as Worker
                        if ((worker.id == workSelected!!.id)) {
                            lv.setItemChecked(position, true)
                            break
                        }
                    }
                }
            }
        }
    }// This tells us the item position we are looking at

    // This tells us the item status at the above position
    // this has no checkboxes
    val selectedItems: List<Any?>
        get() {
            var objSelected: ArrayList<Any>? = ArrayList()
            if (mObjectsToShow != null) {
                if (type == PickerMode.ENCLOSURES || type == PickerMode.FIELDS) {
                    // this has no checkboxes
                    objSelected = mObjectsToShow
                } else {
                    val lv = _v!!.findViewById<ListView>(android.R.id.list)
                    val checkedItems = lv.checkedItemPositions
                    val checkedItemsCount = checkedItems.size()
                    for (i in 0 until checkedItemsCount) {
                        // This tells us the item position we are looking at
                        val position = checkedItems.keyAt(i)

                        // This tells us the item status at the above position
                        val isChecked = checkedItems.valueAt(i)
                        if (isChecked) {
                            objSelected!!.add(mObjectsToShow!![position])
                        }
                    }
                }
            }
            return objSelected!!
        }

    fun setComplete(complete: Boolean) {
        mComplete = complete
    }

    companion object {
        val TAG = "APP/ListPicker"
    }
}