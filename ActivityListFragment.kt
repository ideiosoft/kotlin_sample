package com.agricolum.ui.fragment

import android.content.Context
import android.net.ConnectivityManager
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
import android.view.*
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import com.agricolum.R
import com.agricolum.Util
import com.agricolum.domain.model.AActivity
import com.agricolum.domain.model.Enclosure
import com.agricolum.domain.model.Explotation
import com.agricolum.domain.response.CropColorsModel
import com.agricolum.domain.response.UserDetailsResponse
import com.agricolum.interfaces.ActivityHolder
import com.agricolum.storage.api.AgricolumStore
import com.agricolum.storage.api.CropsResponse.Product__1
import com.agricolum.storage.parser.ActivityDetailParser
import com.agricolum.storage.preferences.AgricolumPreferences
import com.agricolum.ui.activities.MainActivity
import com.agricolum.ui.adapter.ActivityListAdapter
import com.agricolum.ui.contracts.ActivityListContracts
import com.agricolum.ui.fields.EnclosureListPresenter
import com.agricolum.ui.presenter.ActivityListPresenter
import com.agricolum.ui.presenter.ActivityListPresenter.mActivitiesList
import com.agricolum.ui.util.SwipeRefreshLayoutManager
import com.agricolum.ui.util.sortItems
import com.agricolum.ui.view.ActivitiesFilterView
import com.google.gson.Gson
import org.json.JSONException
import org.json.JSONObject
import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class ActivityListFragment : BaseFragment(), ActivityListContracts.View, OnRefreshListener,
    ActivitiesFilterView.FilterViewListener {
    private var presenter: ActivityListContracts.Presenter? = null
    private var _v: View? = null
    private var mSwipeActivities: SwipeRefreshLayout? = null
    private var mListActivities: ListView? = null
    private var mSwipeTasks: SwipeRefreshLayout? = null
    private var mListTasks: ListView? = null
    private var userScrolled = false
    private var mCreateActivityButton: Button? = null
    private var mToggleActivitiesButton: ToggleButton? = null
    private var mToggleTasksButton: ToggleButton? = null
    private var mLabelMessage: TextView? = null
    private var activities_count_msg: TextView? = null
    private var mFilterView: ActivitiesFilterView? = null
    private var adapter: ActivityListAdapter? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onDestroy() {
        if (presenter != null) {
            mFilterView!!.clear()
            presenter!!.onDestroy()
            presenter = null
        }
        super.onDestroy()
    }

    override fun onStart() {
        super.onStart()
        if (presenter == null) {
            presenter = ActivityListPresenter(mActivity, this)
            presenter?.onStart()
        }
    }

    override fun onStop() {
        super.onStop()
        if (presenter != null) {
            presenter!!.onStop()
        }
    }

    override fun onResume() {
        super.onResume()
        adapter = null
        ActivityListPresenter.view = this
        (mActivity as ActivityHolder).setActivityFragment(true)
        trackScreen("ActivityListFragment")
        if (mActivity != null) {
            if (mActivity.getSharedPreferences("crashed", Context.MODE_PRIVATE)
                    .getBoolean("crashed", false)
            ) {
                Util.showLongToast(mActivity, getString(R.string.crash_report))
            }
            mActivity.getSharedPreferences("crashed", Context.MODE_PRIVATE)
                .edit()
                .putBoolean("crashed", false).apply()
        }
        mFilterView!!.refresh()
        presenter!!.onResume()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _v = inflater.inflate(R.layout.fragment_activity_list, container, false)
        return _v
    }

    var mStore: AgricolumStore? = null
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mStore = AgricolumStore.client(context)
        activities_count_msg = _v!!.findViewById(R.id.activities_count_msg)
        mFilterView = _v!!.findViewById(R.id.filter_view)
        mFilterView?.setListener(this)
        if (EnclosureListPresenter.enclosuresNewList != null && EnclosureListPresenter.enclosuresNewList!!.size > 0) {
            if (mFilterView != null) {
                mFilterView!!.setUseSigpac(activity, EnclosureListPresenter.enclosuresNewList)
            }
        } else {
            val enclosuress = mStore?.getEnclosureList(mStore!!.getCurrentUserId())
            mFilterView?.setUseSigpac(activity, enclosuress)
        }
        mSwipeActivities = _v!!.findViewById(R.id.swipe_container_activities)
        mSwipeActivities?.setOnRefreshListener(this)
        SwipeRefreshLayoutManager.setup(mSwipeActivities)
        mSwipeTasks = _v!!.findViewById(R.id.swipe_container_tasks)
        mSwipeTasks?.setOnRefreshListener(this)
        SwipeRefreshLayoutManager.setup(mSwipeTasks)
        mLabelMessage = _v!!.findViewById(R.id.no_activities_msg)
        mCreateActivityButton = _v!!.findViewById(R.id.btn_new_activity)
        mCreateActivityButton?.setOnClickListener(View.OnClickListener { v: View? ->
            (mActivity as ActivityHolder).showAddActivityDialog(
                viewTasks, false
            )
        })
        mToggleActivitiesButton = _v!!.findViewById(R.id.btn_toggle_activities)
        mToggleTasksButton = _v!!.findViewById(R.id.btn_toggle_tasks)
        mToggleActivitiesButton?.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
            if (isChecked) {
                onActivitiesChecked()
            }
        })
        mToggleTasksButton?.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
            if (isChecked) {
                if (ActivityListPresenter.mActivitiesTasksList.size == 0) {
                    ActivityListPresenter.mActivitiesTasksList =
                        mStore?.getTaskList(mStore!!.getCurrentUserId())
                }
                onTasksChecked()
            }
        })
        mListActivities = _v!!.findViewById(R.id.activities_list)
        mListActivities?.setOnItemClickListener(AdapterView.OnItemClickListener { adapterView: AdapterView<*>, view1: View?, position: Int, l: Long ->
            val viewType = mListActivities?.getAdapter()?.getItemViewType(position)
            when (viewType) {
                ActivityListAdapter.ROW_TYPE_ACTIVITY -> {
                    val activity = adapterView.getItemAtPosition(position) as AActivity
                    if (activity.synced) {
                        if (activity != null) {
                            showActivityDetail(activity)
                        }
                    } else {
                    }
                }
            }
        })
        mListActivities?.setOnScrollListener(object : AbsListView.OnScrollListener {
            override fun onScrollStateChanged(view: AbsListView, scrollState: Int) {
                if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
                    userScrolled = true
                } else if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE) {
                    if (userScrolled) {
                        userScrolled = false
                        presenter!!.fetchNextPageActivities()
                    } else {
                        userScrolled = true
                    }
                }
            }

            override fun onScroll(
                view: AbsListView,
                firstVisibleItem: Int,
                visibleItemCount: Int,
                totalItemCount: Int
            ) {
                val diff = totalItemCount - (firstVisibleItem + visibleItemCount)
                if (userScrolled && diff < 5) {
                    if (visibleItemCount > 0) {
                        Log.d(Companion.TAG, "Scroll: reached scroll bottom")
                        userScrolled = false
                        presenter!!.fetchNextPageActivities()
                    }
                }
            }
        })
        mListTasks = _v!!.findViewById(R.id.task_list)
        mListTasks?.setOnItemClickListener(AdapterView.OnItemClickListener { adapterView: AdapterView<*>, view12: View?, position: Int, l: Long ->
            val viewType = mListTasks?.getAdapter()?.getItemViewType(position)
            when (viewType) {
                ActivityListAdapter.ROW_TYPE_ACTIVITY -> {
                    val activity = adapterView.getItemAtPosition(position) as AActivity
                    if (activity.synced) {
                        if (activity != null) {
                            showActivityDetail(activity)
                        }
                    } else {
                        try {
                            if (activity.postJson != null) {
                                val payload = JSONObject(activity.postJson)
                                presenter!!.syncActivity(activity.id, payload)
                            }
                        } catch (ex: JSONException) {
                            ex.printStackTrace()
                        }
                    }
                }
            }
        })
        mListTasks?.setOnScrollListener(object : AbsListView.OnScrollListener {
            override fun onScrollStateChanged(view: AbsListView, scrollState: Int) {
                if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
                    userScrolled = true
                } else if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE) {
                    if (userScrolled) {
                        userScrolled = false
                        presenter!!.fetchNextPageTasks()
                    } else {
                        userScrolled = true
                    }
                }
            }

            override fun onScroll(
                view: AbsListView,
                firstVisibleItem: Int,
                visibleItemCount: Int,
                totalItemCount: Int
            ) {
                val diff = totalItemCount - (firstVisibleItem + visibleItemCount)
                if (userScrolled && diff < 5) {
                    if (visibleItemCount > 0) {
                        Log.e(Companion.TAG, "Scroll: reached scroll bottom")
                        userScrolled = false
                        presenter!!.fetchNextPageTasks()
                    }
                }
            }
        })
        if (!viewTasks) {
            // set initial state for activities
            mToggleActivitiesButton?.setChecked(true)
            mToggleActivitiesButton?.setClickable(false)
            mToggleTasksButton?.setChecked(false)
            mToggleTasksButton?.setClickable(true)
            mCreateActivityButton?.setText(R.string.create_activity)
            val plannedLay = mFilterView?.findViewById<RadioGroup>(R.id.plannedLay)
            plannedLay?.visibility = View.GONE
            val noCropCheckBox = mFilterView?.findViewById<CheckBox>(R.id.noCropCheckBox)
            noCropCheckBox?.visibility = View.GONE
        } else {
            // set initial state for tasks
            mToggleActivitiesButton?.setChecked(false)
            mToggleActivitiesButton?.setClickable(true)
            mToggleTasksButton?.setChecked(true)
            mToggleTasksButton?.setClickable(false)
            val plannedLay = mFilterView?.findViewById<RadioGroup>(R.id.plannedLay)
            plannedLay?.visibility = View.VISIBLE
            mCreateActivityButton?.setText(R.string.create_task)
        }
    }

    override fun startLoading() {
        if (viewTasks) {
            if (mSwipeTasks != null) {
                mSwipeTasks!!.isRefreshing = true
            }
        } else {
            if (mSwipeActivities != null) {
                mSwipeActivities!!.isRefreshing = true
            }
        }
    }

    override fun stopLoading() {
        if (activity != null) {
            requireActivity().runOnUiThread {
                if (mSwipeActivities != null) {
                    if (mSwipeActivities!!.isRefreshing) {
                        mSwipeActivities!!.isRefreshing = false
                    }
                }
                if (mSwipeTasks != null) {
                    if (mSwipeTasks!!.isRefreshing) {
                        mSwipeTasks!!.isRefreshing = false
                    }
                }
            }
        }
    }

    override fun showErrorMessage(message: String?) {
        Util.showToast(context, message)
    }

    override fun showInfoMessage(message: String?) {
        Util.showToast(context, message)
    }

    override fun displayActivities(activities: List<AActivity>?) {
        if (!isAdded) {
            return
        }
        if (!viewTasks) {
            activities_count_msg!!.setText("Acitivities -"+ mActivitiesList!!.size!!.toString())
            Log.e("vvvvv", mActivitiesList!!.size!!.toString())
           // Toast.makeText(context, mActivitiesList!!.size!!.toString())
            if (activities!!.isEmpty()) {
                mLabelMessage!!.visibility = View.VISIBLE
                mLabelMessage!!.setText(R.string.no_activities)
            } else {
                mLabelMessage!!.visibility = View.GONE
            }
        }
        if (adapter != null) {
            adapter!!.notifyDataSetChanged()
        } else {
            if (mActivity != null) {
                val set = HashSet(activities)
                val list2: List<AActivity> = ArrayList(set)
                val adapter = ActivityListAdapter(
                    mActivity,
                    generateActivityHeaders(activities!!),
                    mActivity.layoutInflater
                )
                mListActivities!!.adapter = adapter
            }
        }
    }

    override fun displayActivitiesTemp(activities: List<AActivity>?) {
        if (activity != null) {
            requireActivity().runOnUiThread {
                if (!viewTasks) {
                    activities_count_msg!!.setText("Acitivities -"+ mActivitiesList!!.size!!.toString())
                    if (activities!!.isEmpty()) {
                        mLabelMessage!!.visibility = View.VISIBLE
                        mLabelMessage!!.setText(R.string.no_activities)
                    } else {
                        mLabelMessage!!.visibility = View.GONE
                    }
                }
                val set = HashSet(activities)
                val list2: List<AActivity> = ArrayList(set)
                if (mActivity != null) {
                    adapter = ActivityListAdapter(
                        mActivity,
                        generateActivityHeaders(activities!!),
                        mActivity.layoutInflater
                    )
                    mListActivities!!.adapter = adapter
                }
            }
        }
    }

    override fun displayActivitiesFiltered(activitiesFiltered: List<AActivity>?) {
        if (!isAdded) {
            return
        }
        if (!viewTasks) {
            if (activitiesFiltered!!.isEmpty()) {
                mLabelMessage!!.visibility = View.VISIBLE
                mLabelMessage!!.setText(R.string.no_activities)
            } else {
                mLabelMessage!!.visibility = View.GONE
            }
        }
        val set = HashSet(activitiesFiltered)
        val list2: List<AActivity> = ArrayList(set)
        if (mActivity != null) {
            val adapter = ActivityListAdapter(
                mActivity,
                generateActivityHeaders(activitiesFiltered!!),
                mActivity.layoutInflater
            )
            mListActivities!!.adapter = adapter
        }
    }

    override fun appendActivities(activities: List<AActivity>?) {
        if (!isAdded) { // avoid crashing when view is not displayed
            return
        }
        activities_count_msg!!.setText("Acitivities -"+ mActivitiesList!!.size!!.toString())
        requireActivity().runOnUiThread {
            val adapter = mListActivities!!.adapter as ActivityListAdapter
            if (generateActivityHeaders(activities!!) != null) {
                adapter.addAll(generateActivityHeaders(activities!!))
            }
            adapter.notifyDataSetChanged()
        }
    }

    override fun showActivityDetail(activityy: AActivity?) {
        try {
            val parser = ActivityDetailParser()
            val detail = ActivityDetailParser.parse(activityy!!.payload)
            if (detail != null) {
                (mActivity as ActivityHolder).showActivityDetail(detail)
            }
        } catch (ex: JSONException) {
            ex.printStackTrace()
        }
    }

    override fun displayTasks(tasks: List<AActivity>?) {
        if (!isAdded) { // avoid crashing when view is not displayed
            return
        }
        if (viewTasks) {
            activities_count_msg!!.setText("Planned Acitivities -"+ ActivityListPresenter.mActivitiesTasksList!!.size!!.toString())
            if (tasks!!.isEmpty()) {
                mLabelMessage!!.visibility = View.VISIBLE
                mLabelMessage!!.setText(R.string.no_tasks)
            } else {
                mLabelMessage!!.visibility = View.GONE
            }
        }
        val adapter =
            ActivityListAdapter(mActivity, generateTasksHeaders(tasks!!), mActivity.layoutInflater)


        mActivity.runOnUiThread(Runnable {
            if(mListTasks!=null)
            {
                mListTasks?.adapter = adapter
            }

            // Stuff that updates the UI
        })

    }

    override fun populateCropsFilterView(cropsList: List<Product__1>?) {
        mFilterView!!.setCrops(activity, cropsList)
    }

    override fun appendTasks(tasks: List<AActivity>?) {
        if (!isAdded) { // avoid crashing when view is not displayed
            return
        }
        val adapter = mListTasks!!.adapter as ActivityListAdapter
        activities_count_msg!!.setText("Planned Acitivities -"+ ActivityListPresenter.mActivitiesTasksList!!.size!!.toString())
        if (generateTasksHeaders(tasks!!) != null) {
            adapter.addAll(generateTasksHeaders(tasks!!))
        }
        adapter.notifyDataSetChanged()
    }

    override fun onUserDetails(it: UserDetailsResponse?) {
        if (it!!.userDetails != null) {
            if (it.userDetails.activityItemsColors != null && requireActivity() != null) {
                AgricolumPreferences.putUserDetails(
                    requireActivity(),
                    Gson().toJson(it.userDetails.activityItemsColors)
                )
            }
            if (it.userDetails.currentSubscription != null) {
                AgricolumPreferences.putUserCostDetails(
                    requireActivity(),
                    it.userDetails.currentSubscription.costs
                )
            }
        }
    }

    private fun generateTasksHeaders(tasks: List<AActivity>): List<AActivity> {
        Collections.sort(tasks, sortItems())
        return generateHeaders(tasks, true)
    }

    private fun generateActivityHeaders(activities: List<AActivity>): List<AActivity> {
        return generateHeaders(activities, false)
    }

    private fun generateHeaders(items: List<AActivity>?, isTask: Boolean): List<AActivity> {
        var lastDate: String? = null
        val elementsToAdd: MutableList<AActivity> = ArrayList()
        if (items != null) {
            for (i in items.indices) {
                val aActivity = items[i]
                if (!isTask && aActivity.is_task || isTask && !aActivity.is_task) {
                    continue
                }
                var dateString: String
                dateString = if (isTask) {
                    Util.getDateRange(aActivity.start_date, aActivity.end_date)
                } else {
                    Util.getDateString(aActivity.start_date)
                }
                if (dateString != lastDate) {
                    val separator = AActivity()
                    separator.isSeparator = true
                    separator.activity_type_name = dateString
                    lastDate = separator.activity_type_name
                    elementsToAdd.add(separator)
                }
                elementsToAdd.add(aActivity)
            }
        }
        return elementsToAdd
    }

    private fun onActivitiesChecked() {
        viewTasks = false
        mToggleTasksButton!!.isChecked = false
        mToggleTasksButton!!.isClickable = true
        mToggleActivitiesButton!!.isClickable = false
        mSwipeActivities!!.visibility = View.VISIBLE
        mListActivities!!.visibility = View.VISIBLE
        mSwipeTasks!!.visibility = View.GONE
        mListTasks!!.visibility = View.GONE
        val plannedLay = mFilterView!!.findViewById<RadioGroup>(R.id.plannedLay)
        plannedLay.visibility = View.GONE
        mCreateActivityButton!!.setText(R.string.create_activity)
        if (presenter != null) {
            presenter!!.fetchActivities(false)
        }
    }

    private fun onTasksChecked() {
        viewTasks = true
        mToggleActivitiesButton!!.isChecked = false
        mToggleActivitiesButton!!.isClickable = true
        mToggleTasksButton!!.isClickable = false
        mSwipeActivities!!.visibility = View.GONE
        mListActivities!!.visibility = View.GONE
        mSwipeTasks!!.visibility = View.VISIBLE
        mListTasks!!.visibility = View.VISIBLE
        val plannedLay = mFilterView!!.findViewById<RadioGroup>(R.id.plannedLay)
        plannedLay.visibility = View.VISIBLE
        val zvLay = mFilterView!!.findViewById<LinearLayout>(R.id.zvLay)
        zvLay.visibility = View.GONE
        val srLay = mFilterView!!.findViewById<LinearLayout>(R.id.srLay)
        srLay.visibility = View.GONE
        mCreateActivityButton!!.setText(R.string.create_task)
        if (presenter != null) {
            presenter!!.fetchTasks(false)
        }
    }

    override fun onRefresh() {
        syncActivities()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.activities, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    private val isNetworkConnected: Boolean
        private get() {
            val cm =
                requireActivity().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            return cm.activeNetworkInfo != null && cm.activeNetworkInfo!!.isConnected
        }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_sync -> {
                if (isNetworkConnected) {
                    syncActivities()
                } else {
                    showInfoMessage("No internet")
                }
                true
            }
            R.id.action_activities_map -> {
                (mActivity as MainActivity).showActivitiesMap()
                true
            }
            R.id.action_activities_filter -> {
                MainActivity.mNoCropVisiblityStatus = false
                showHideFilter()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun syncActivities() {
        if (isNetworkConnected) {
            trackAction("Refreshing all", AgricolumPreferences.getUser(mActivity))
            ActivityListPresenter.mActivitiesList = null
            presenter!!.onRefreshList()
            presenter!!.sync()
        }
    }

    override fun populateActivitiesFilterView(activitiesList: List<AActivity>?) {
        if (activitiesList != null) {
            mFilterView!!.setActivities(context, activitiesList)
        }
    }

    override fun populateExplotationsFilterView(explotationsList: List<Explotation>?) {
        if (explotationsList != null) {
            MainActivity.mActivityExplotationCheckedTemp.clear()
            MainActivity.mActivityExplotationCheckedTemp.addAll(MainActivity.mActivityExplotationChecked)
            mFilterView!!.setExplotations(
                context,
                explotationsList,
                MainActivity.mActivityExplotationChecked
            )
        }
    }

    private fun explotationsCheckedContains(
        explotationsChecked: List<Explotation>,
        value: String
    ): Boolean {
        for (explotation in explotationsChecked) {
            if (explotation.id == value || explotation.name!!.split(",").toTypedArray()[0].equals(
                    value,
                    ignoreCase = true
                )
            ) {
                return true
            }
        }
        return false
    }

    private fun activityCheckedContains(value: String): Boolean {
        for (explotation in MainActivity.mActivitiesChecked) {
            if (explotation == value) {
                return true
            }
        }
        return false
    }

    override fun filterData(
        startDate: Long?,
        endDate: Long?,
        mCurrentPageActivities: Int,
        FETCH_SIZE: Int,
        mActivitiesListNew: ArrayList<AActivity>?,
        mExplotationsChecked: List<Explotation>?
    ) {
        var mActivitiesListNew: ArrayList<AActivity>? = mActivitiesListNew
        val mActivitiesListNewTemp = ArrayList<AActivity>()
        val offset = (mCurrentPageActivities - 1) * FETCH_SIZE
        mActivitiesListNew = ArrayList()
        val finalMActivitiesListNew: ArrayList<AActivity> = mActivitiesListNew
        val handler: Handler
        val finalMActivitiesListNew1 = finalMActivitiesListNew
        handler = object : Handler() {
            override fun handleMessage(msg: Message) {
                if (activity != null) {
                    activity!!.runOnUiThread {
                        ActivityListPresenter.mActivitiesListNew.clear()
                        ActivityListPresenter.mActivitiesListNew.addAll(mActivitiesListNewTemp)
                        if (offset + FETCH_SIZE > mActivitiesListNewTemp.size) {
                            displayActivitiesFiltered(
                                mActivitiesListNewTemp.subList(
                                    offset,
                                    mActivitiesListNewTemp.size
                                )
                            )
                        } else {
                            displayActivitiesFiltered(
                                mActivitiesListNewTemp.subList(
                                    offset,
                                    offset + FETCH_SIZE
                                )
                            )
                        }
                    }
                }
            }
        }
        Thread {
            if (mActivity != null) {
                val activitiesTmp: MutableList<AActivity> = ArrayList()
                Log.e("before filter", "-> " + ActivityListPresenter.mActivitiesList.size)
                if (ActivityListPresenter.mActivitiesList != null) {
                    val activitiesTmppp: MutableList<AActivity> = ArrayList()
                    for (i in ActivityListPresenter.mActivitiesList.indices) {
                        val activity = ActivityListPresenter.mActivitiesList[i]
                        val str_date = activity.start_date
                        val formatter: DateFormat = SimpleDateFormat("yyyy-MM-dd")
                        var date: Date? = null
                        try {
                            date = formatter.parse(str_date.split("T").toTypedArray()[0]) as Date
                        } catch (e: ParseException) {
                            e.printStackTrace()
                        }
                        if (activity.end_date != null && !activity.end_date.equals(
                                "",
                                ignoreCase = true
                            )
                        ) {
                            val end_date = activity.end_date
                            val formatterr: DateFormat = SimpleDateFormat("yyyy-MM-dd")
                            var datee: Date? = null
                            try {
                                datee =
                                    formatterr.parse(end_date.split("T").toTypedArray()[0]) as Date
                            } catch (e: ParseException) {
                                e.printStackTrace()
                            }
                            if (date != null && startDate != null && endDate != null) {
                                if (date.time >= startDate && date.time <= endDate && datee!!.time >= startDate && datee.time <= endDate) {
                                    if (!activitiesTmppp.contains(activity)) {
                                        activitiesTmppp.add(activity)
                                    }
                                }
                            } else {
                                activitiesTmppp.add(activity)
                            }
                        }
                    }
                    Log.e("after date filter", "-> " + activitiesTmppp.size)
                    activitiesTmp.clear()
                    activitiesTmp.addAll(activitiesTmppp)
                    for (h in activitiesTmp.indices) {
                        val activity = activitiesTmp[h]
                        val explotationNames = activity.explotations
                        if (explotationNames != null && explotationNames.size > 1) {
                            for (explotationName in explotationNames) {
                                if (explotationsCheckedContains(
                                        MainActivity.mActivityExplotationChecked,
                                        explotationName.id!!
                                    ) && activityCheckedContains(activity.activity_type_name)
                                ) {
                                    if (!finalMActivitiesListNew.contains(activity)) {
                                        finalMActivitiesListNew.add(activity)
                                    }
                                } else if (mExplotationsChecked?.size == MainActivity.mActivityExplotationChecked.size && ActivitiesFilterView.mSelectableActivitiesList.size == MainActivity.mActivitiesChecked.size) {
                                    if (!finalMActivitiesListNew.contains(activity)) {
                                        finalMActivitiesListNew.add(activity)
                                    }
                                } else if (explotationsCheckedContains(
                                        MainActivity.mActivityExplotationChecked,
                                        explotationName.id!!
                                    ) && ActivitiesFilterView.mSelectableActivitiesList.size == MainActivity.mActivitiesChecked.size
                                ) {
                                    if (!finalMActivitiesListNew.contains(activity)) {
                                        finalMActivitiesListNew.add(activity)
                                    }
                                } else if (mExplotationsChecked?.size == MainActivity.mActivityExplotationChecked.size && activityCheckedContains(
                                        activity.activity_type_name
                                    )
                                ) {
                                    if (!finalMActivitiesListNew.contains(activity)) {
                                        finalMActivitiesListNew.add(activity)
                                    }
                                } else if (mExplotationsChecked?.size == MainActivity.mActivityExplotationChecked.size && MainActivity.mActivitiesChecked.size == 0) {
                                    if (!finalMActivitiesListNew.contains(activity)) {
                                        finalMActivitiesListNew.add(activity)
                                    }
                                } else if (explotationsCheckedContains(
                                        MainActivity.mActivityExplotationChecked,
                                        explotationName.id!!
                                    ) && MainActivity.mActivitiesChecked.size == 0
                                ) {
                                    if (!finalMActivitiesListNew.contains(activity)) {
                                        finalMActivitiesListNew.add(activity)
                                    }
                                }
                            }
                        } else if (explotationNames != null && explotationNames.size == 1) {
                            for (explotationName in explotationNames) {
                                if (explotationsCheckedContains(
                                        MainActivity.mActivityExplotationChecked,
                                        explotationName.id!!
                                    ) &&
                                    activityCheckedContains(activity.activity_type_name)
                                ) {
                                    if (!finalMActivitiesListNew.contains(activity)) {
                                        finalMActivitiesListNew.add(activity)
                                    }
                                } else if (mExplotationsChecked?.size == MainActivity.mActivityExplotationChecked.size && ActivitiesFilterView.mSelectableActivitiesList.size == MainActivity.mActivitiesChecked.size) {
                                    if (!finalMActivitiesListNew.contains(activity)) {
                                        finalMActivitiesListNew.add(activity)
                                    }
                                } else if (explotationsCheckedContains(
                                        MainActivity.mActivityExplotationChecked,
                                        explotationName.id!!
                                    ) && ActivitiesFilterView.mSelectableActivitiesList.size == MainActivity.mActivitiesChecked.size
                                ) {
                                    if (!finalMActivitiesListNew.contains(activity)) {
                                        finalMActivitiesListNew.add(activity)
                                    }
                                } else if (mExplotationsChecked?.size == MainActivity.mActivityExplotationChecked.size && activityCheckedContains(
                                        activity.activity_type_name
                                    )
                                ) {
                                    if (!finalMActivitiesListNew.contains(activity)) {
                                        finalMActivitiesListNew.add(activity)
                                    }
                                } else if (explotationsCheckedContains(
                                        MainActivity.mActivityExplotationChecked,
                                        explotationName.id!!
                                    ) && MainActivity.mActivitiesChecked.size == 0
                                ) {
                                    if (!finalMActivitiesListNew.contains(activity)) {
                                        finalMActivitiesListNew.add(activity)
                                    }
                                }
                            }
                        } else if (explotationNames != null) {
                            for (explotationName in explotationNames) {
                                if (mExplotationsChecked?.size == MainActivity.mActivityExplotationChecked.size && ActivitiesFilterView.mSelectableActivitiesList.size == MainActivity.mActivitiesChecked.size) {
                                    if (!finalMActivitiesListNew.contains(activity)) {
                                        finalMActivitiesListNew.add(activity)
                                    }
                                } else if (mExplotationsChecked?.size == MainActivity.mActivityExplotationChecked.size && activityCheckedContains(
                                        activity.activity_type_name
                                    )
                                ) {
                                    if (!finalMActivitiesListNew.contains(activity)) {
                                        finalMActivitiesListNew.add(activity)
                                    }
                                } else if (explotationsCheckedContains(
                                        MainActivity.mActivityExplotationChecked,
                                        explotationName.id!!
                                    ) && MainActivity.mActivitiesChecked.size == 0
                                ) {
                                    if (!finalMActivitiesListNew.contains(activity)) {
                                        finalMActivitiesListNew.add(activity)
                                    }
                                }
                            }
                        }
                    }
                    if (MainActivity.mColorCropsChecked.size != 0) {
                        if (MainActivity.mColorCropsChecked.size != ActivitiesFilterView.newColorListCrop.size) {
                            for (i in MainActivity.mColorCropsChecked.indices) {
                                for (j in finalMActivitiesListNew.indices) {
                                    if (finalMActivitiesListNew[j].products != null) {
                                        for (k in finalMActivitiesListNew[j].products.indices) {
                                            if (finalMActivitiesListNew[j].products[k].product_id.equals(
                                                    MainActivity.mColorCropsChecked[i].itemId,
                                                    ignoreCase = true
                                                )
                                            ) {
                                                if (!mActivitiesListNewTemp.contains(
                                                        finalMActivitiesListNew[j]
                                                    )
                                                ) {
                                                    mActivitiesListNewTemp.add(
                                                        finalMActivitiesListNew[j]
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            mActivitiesListNewTemp.addAll(finalMActivitiesListNew)
                        }
                    } else {
                        mActivitiesListNewTemp.addAll(finalMActivitiesListNew)
                    }
                    Log.e("after crop filter", "-> " + mActivitiesListNewTemp.size)
                    finalMActivitiesListNew.clear()
                    finalMActivitiesListNew.addAll(mActivitiesListNewTemp)
                    val mActivitiesListNewTemp2 = ArrayList<AActivity>()
                    if (MainActivity.mUseSigpacAll.size == MainActivity.selectedUseSigpacFilter.size || MainActivity.selectedUseSigpacFilter.size == 0) {
                        Log.e("after sig 1 filter", "-> " + finalMActivitiesListNew.size)
                        mActivitiesListNewTemp2.addAll(finalMActivitiesListNew)
                    } else {
                        for (j in MainActivity.selectedUseSigpacFilter.indices) {
                            for (k in finalMActivitiesListNew.indices) {
                                if (finalMActivitiesListNew[k].zones != null) {
                                    for (i in finalMActivitiesListNew[k].zones.indices) {
                                        if (MainActivity.selectedUseSigpacFilter[j].id.equals(
                                                finalMActivitiesListNew[k].zones[i].zoneable_id,
                                                ignoreCase = true
                                            )
                                        ) {
                                            if (!mActivitiesListNewTemp2.contains(
                                                    finalMActivitiesListNew[k]
                                                )
                                            ) {
                                                mActivitiesListNewTemp2.add(
                                                    finalMActivitiesListNew[k]
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Log.e("after sig 2 filter", "-> " + mActivitiesListNewTemp2.size)
                    finalMActivitiesListNew.clear()
                    finalMActivitiesListNew.addAll(mActivitiesListNewTemp2)
                    mActivitiesListNewTemp.clear()
                    mActivitiesListNewTemp.addAll(finalMActivitiesListNew)
                    ActivityListPresenter.mActivitiesListNew = ArrayList()
                    ActivityListPresenter.mActivitiesListNew.addAll(finalMActivitiesListNew)
                    handler.handleMessage(Message())
                }
            }
        }.start()
    }

    override fun onClickColor(id: String, s: String) {
        presenter!!.saveExplotationColor(id, s, "stroke_color")
    }

    override fun onCropClickColor(activityId: String, itemId: String, mode: String, color: String) {
        presenter!!.saveCropColor(activityId, itemId, mode, color)
    }

    override fun onClickFilterView(
        startDate: Long,
        endDate: Long,
        explotationsChecked: List<Explotation>,
        activitiesChecked: List<String>,
        allEnclosures: Boolean,
        allEnclosuresWithTasks: Boolean,
        enclosureWith100: Boolean,
        enclosureWithOut100: Boolean,
        cropColorChecked: List<CropColorsModel>,
        useSigpacChecked: List<Enclosure>,
        cropsChecked: List<String>,
        noCropChecked: Boolean
    ) {
        MainActivity.mNoCropChecked = noCropChecked
        mFilterView!!.animation =
            AnimationUtils.loadAnimation(context, R.anim.slide_down)
        mFilterView!!.visibility = View.GONE
        MainActivity.mActivitiesChecked = ArrayList()
        if (EnclosureListPresenter.mCropsChecked == null) {
            EnclosureListPresenter.mCropsChecked = ArrayList()
        }
        EnclosureListPresenter.mCropsChecked?.clear()
        EnclosureListPresenter.mCropsChecked?.addAll(cropsChecked)
        if (MainActivity.selectedUseSigpacFilter == null) {
            MainActivity.selectedUseSigpacFilter = ArrayList()
        }
        MainActivity.selectedUseSigpacFilter.clear()
        MainActivity.selectedUseSigpacFilter.addAll(useSigpacChecked)
        MainActivity.mCropsCheckedCountCheck = ArrayList()
        MainActivity.mCropsCheckedCountCheck.addAll(cropsChecked)
        MainActivity.mActivitiesChecked = activitiesChecked
        MainActivity.mActivityExplotationChecked = ArrayList()
        MainActivity.mEnclosuresChecked = ArrayList()
        MainActivity.mActivityExplotationChecked = explotationsChecked
        MainActivity.mEnclosuresChecked = explotationsChecked
        MainActivity.mCropsChecked = ArrayList()
        MainActivity.mCropsChecked.addAll(cropsChecked)
        MainActivity.mColorCropsChecked = ArrayList()
        MainActivity.mColorCropsChecked.addAll(cropColorChecked)
        if (viewTasks) {
            presenter!!.filterPlannedList(
                startDate,
                endDate,
                explotationsChecked,
                activitiesChecked,
                allEnclosures,
                allEnclosuresWithTasks,
                enclosureWith100,
                enclosureWithOut100
            )
        } else {
            MainActivity.mCropsChecked = ArrayList()
            MainActivity.mCropsChecked.addAll(cropsChecked)
            MainActivity.mColorCropsChecked = ArrayList()
            MainActivity.mColorCropsChecked.addAll(cropColorChecked)
            EnclosureListPresenter.mCropsChecked?.clear()
            EnclosureListPresenter.mCropsChecked?.addAll(cropsChecked)
            if (MainActivity.mActivitiesChecked.size == 0 && MainActivity.mActivityExplotationChecked.size == 0) {
                presenter!!.fetchActivities(false)
            } else if (MainActivity.mActivitiesChecked.size == 0 && ActivityListPresenter.mExplotationsChecked.size == MainActivity.mActivityExplotationChecked.size) {
                presenter!!.fetchActivities(false)
            } else if (MainActivity.mActivitiesChecked.size == ActivitiesFilterView.mSelectableActivitiesList.size && ActivityListPresenter.mExplotationsChecked.size == MainActivity.mActivityExplotationChecked.size && MainActivity.mCropsChecked.size == EnclosureListPresenter.tempCropFilterSize) {
                presenter!!.fetchActivities(false)
            } else {
                presenter!!.filterActivitiesList(
                    startDate,
                    endDate,
                    explotationsChecked,
                    activitiesChecked,
                    MainActivity.mCropsChecked,
                    useSigpacChecked
                )
            }
        }
    }

    private fun showHideFilter() {
        if (mFilterView!!.visibility == View.VISIBLE) {
            mFilterView!!.animation = AnimationUtils.loadAnimation(context, R.anim.slide_down)
            mFilterView!!.visibility = View.GONE
        } else {
            if (EnclosureListPresenter.enclosuresNewList != null && EnclosureListPresenter.enclosuresNewList!!.size > 0) {
                if (mFilterView != null) {
                    mFilterView!!.setUseSigpac(activity, EnclosureListPresenter.enclosuresNewList)
                }
            } else {
                val enclosuress = mStore!!.getEnclosureList(
                    mStore!!.currentUserId
                )
                mFilterView!!.setUseSigpac(activity, enclosuress)
            }
            populateExplotationsFilterView(ActivityListPresenter.mExplotationsChecked)
            mFilterView!!.animation = AnimationUtils.loadAnimation(context, R.anim.slide_up)
            mFilterView!!.visibility = View.VISIBLE
        }
    }

    companion object {
        protected const val TAG = "APP/ACTIVITIES"
        @JvmField
        var viewTasks = false
    }
}