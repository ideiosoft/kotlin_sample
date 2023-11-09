package com.agricolum.ui.fragment

import android.Manifest
import com.agricolum.ui.fragment.BaseDialogFragment
import com.agricolum.ui.contracts.ChooseSprayingContracts
import com.agricolum.interfaces.OnAdapterItemClick
import com.agricolum.interfaces.OnAuthAdapterItemClick
import android.app.ProgressDialog
import com.agricolum.domain.model.Chemical
import com.agricolum.domain.model.Plague
import androidx.recyclerview.widget.RecyclerView
import com.agricolum.ui.fragment.ActivityEditSprayingDialog.NoticeSpryingDialogListener
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import com.agricolum.domain.model.PurchasesModel.PurchaseItem
import com.androidbuts.multispinnerfilter.KeyPairBoolData
import com.agricolum.storage.api.AuthorizedUsesResponse.AuthorizedUse
import com.agricolum.R
import android.os.Bundle
import com.agricolum.ui.activities.MainActivity
import com.agricolum.ui.presenter.ChooseSprayingPresenter
import com.google.gson.Gson
import android.text.TextWatcher
import android.text.Editable
import android.graphics.drawable.ColorDrawable
import androidx.recyclerview.widget.LinearLayoutManager
import android.content.pm.PackageManager
import com.agricolum.ui.fragment.ActivityEditSprayingDialog
import com.agricolum.domain.model.DropdownItem
import android.view.View.OnTouchListener
import android.view.MotionEvent
import com.agricolum.domain.model.PurchasesModel.Purchase
import com.androidbuts.multispinnerfilter.MultiSpinnerSearch
import com.agricolum.ui.fragment.ActivityEditProductDialog
import com.agricolum.adapter.SelectedProductsAdapter
import com.agricolum.ui.adapter.ChemicalSpinnerAdapter
import android.view.View.OnFocusChangeListener
import com.agricolum.adapter.AuthUsesAdapter
import com.agricolum.domain.model.SprayingAttribute
import android.os.AsyncTask
import com.agricolum.FileDownloader
import android.content.Intent
import android.os.Build
import android.os.Build.VERSION_CODES
import android.content.ActivityNotFoundException
import android.graphics.Color
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.view.View
import android.view.Window
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.core.view.MotionEventCompat
import com.agricolum.Util
import com.agricolum.interfaces.Holder
import java.io.File
import java.io.IOException
import java.lang.Exception
import java.util.ArrayList

class ActivityEditSprayingDialog : BaseDialogFragment(), ChooseSprayingContracts.View,
    OnAdapterItemClick, OnAuthAdapterItemClick {
    var progressDialog: ProgressDialog? = null
    var presenter: ChooseSprayingContracts.Presenter? = null
    var _v: View? = null
    var mChemicals: List<Chemical>? = null
    var mPlagues: List<Plague>? = null
    var mPlagueSelectedAuth: Plague? = null
    var mCropSelectedAuth: String? = null
    var authRv: RecyclerView? = null
    var recomendedLay: LinearLayout? = null
    var treatmentRadioGroup: RadioGroup? = null
    var alternativeLabel: TextView? = null
    var productStockLabel: TextView? = null
    var chooseProductTv: TextView? = null
    var fertilizer_label: TextView? = null
    var activityEdit_alternativeProduct: EditText? = null
    var progress: RelativeLayout? = null
    var mChemicalSelectedAuth: Chemical? = null

    // Use this instance of the interface to deliver action events
    var mListener: NoticeSpryingDialogListener? = null
    private val mActivity: Activity? = null
    private var mChemicalSelected: Chemical? = null
    private var mPlagueSelected: Plague? = null
    var items: ArrayList<PurchaseItem>? = null
    var listArray1: ArrayList<KeyPairBoolData>? = null
      var dialogg: Dialog? = null
    private var mSelectedAuth: AuthorizedUse? = null
    override fun onQuantityDataChange() {}
    override fun onAuthItemClick(data: AuthorizedUse) {
        mSelectedAuth = data
        dialogg!!.dismiss()
        recomendedLay!!.visibility = View.VISIBLE
        for (i in mPlagues!!.indices) {
            if (data.plagueId.toString().equals(mPlagues!![i].id, ignoreCase = true)) {
                mPlagueSelected = mPlagues!![i]
                break
            }
            //strArray[i] = mPlagues.get(i).name;
        }
        for (i in mChemicals!!.indices) {
            if (data.chemicalId.toString().equals(mChemicals!![i].id, ignoreCase = true)) {
                mChemicalSelected = mChemicals!![i]
                break
            }
            //strArray[i] = mPlagues.get(i).name;
        }
        val xx = _v!!.findViewById<AutoCompleteTextView>(R.id.activityEdit_spryingChemicalsA)
        val xxx = _v!!.findViewById<AutoCompleteTextView>(R.id.activityEdit_spryingPlagueA)
        xx.setText(data.chemical_full_name)
        xxx.setText(data.infestation)
        (_v!!.findViewById<View>(R.id.doseValue) as TextView).text =
            data.minValue.toString() + " - " + data.maxValue + " " + data.unit
    }

    /*
     * The activity that creates an instance of this dialogg fragment must
     * implement this interface in order to receive event callbacks. Each method
     * passes the DialogFragment in case the host needs to query it.
     */
    interface NoticeSpryingDialogListener {
        fun onSaveSprayingDialogClick()
    }

    fun setListener(listener: NoticeSpryingDialogListener?) {
        mListener = listener
    }

    override fun onAttach(activity: Activity) {
        super.onAttach(activity)
        mActivity = activity
    }

    override fun onDetach() {
        super.onDetach()
        mActivity = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onDestroy() {
        (mActivity as MainActivity).showingDialog = false
        if (presenter != null) {
            presenter!!.onDestroy()
            presenter = null
        }
        super.onDestroy()
    }

    override fun onStart() {
        super.onStart()
        if (presenter == null) {
            presenter = ChooseSprayingPresenter(mActivity, this)
            presenter?.fetchPurchases(true)
        }
    }

    override fun onStop() {
        super.onStop()
        if (presenter != null) {
            presenter!!.cancelTasks()
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(activity)

        // Inflate and set the layout for the dialogg
        // Pass null as the parent view because its going in the dialogg layout
        _v = requireActivity().layoutInflater.inflate(R.layout.dialog_activityedit_spraying, null)

        // set title
        (_v?.findViewById<View>(R.id.dlg_activityEdit_title) as TextView).text =
            getString(R.string.activityEdit_sprayingsAttributesTxt)
        recomendedLay = _v?.findViewById(R.id.recomendedLay)
        treatmentRadioGroup = _v?.findViewById(R.id.treatmentRadioGroup)
        productStockLabel = _v?.findViewById(R.id.productStockLabel)
        chooseProductTv = _v?.findViewById(R.id.chooseProductTv)
        fertilizer_label = _v?.findViewById(R.id.fertilizer_label)
        alternativeLabel = _v?.findViewById(R.id.alternativeLabel)
        activityEdit_alternativeProduct = _v?.findViewById(R.id.activityEdit_alternativeProduct)
        treatmentRadioGroup?.setOnCheckedChangeListener(RadioGroup.OnCheckedChangeListener { group, checkedId ->
            if (checkedId == R.id.chemicalTreatment) {
                _v!!.findViewById<View>(R.id.vvv).visibility = View.VISIBLE
                _v!!.findViewById<View>(R.id.chooseAuthorizedUses).visibility = View.VISIBLE
                _v!!.findViewById<View>(R.id.activityEdit_spryingChemicalsA).visibility = View.VISIBLE
                _v!!.findViewById<View>(R.id.activityEdit_spryingAmountScope).visibility =
                    View.VISIBLE
                _v!!.findViewById<View>(R.id.amountScopeLabel).visibility = View.VISIBLE
                fertilizer_label?.setVisibility(View.VISIBLE)
                alternativeLabel?.setVisibility(View.GONE)
                _v!!.findViewById<View>(R.id.unitsLabel).visibility = View.GONE
                _v!!.findViewById<View>(R.id.unitsValue).visibility = View.GONE
                activityEdit_alternativeProduct?.setVisibility(View.GONE)
            } else {
                _v!!.findViewById<View>(R.id.unitsLabel).visibility = View.VISIBLE
                _v!!.findViewById<View>(R.id.unitsValue).visibility = View.VISIBLE
                _v!!.findViewById<View>(R.id.amountScopeLabel).visibility = View.GONE
                _v!!.findViewById<View>(R.id.activityEdit_spryingAmountScope).visibility = View.GONE
                _v!!.findViewById<View>(R.id.vvv).visibility = View.GONE
                _v!!.findViewById<View>(R.id.chooseAuthorizedUses).visibility = View.GONE
                _v!!.findViewById<View>(R.id.activityEdit_spryingChemicalsA).visibility = View.GONE
                fertilizer_label?.setVisibility(View.GONE)
                alternativeLabel?.setVisibility(View.VISIBLE)
                activityEdit_alternativeProduct?.setVisibility(View.VISIBLE)
            }
        })
        val amount = _v?.findViewById<EditText>(R.id.activityEdit_spryingAmount)
        val recoLabel = _v?.findViewById<TextView>(R.id.recoLabel)
        amount?.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}
            override fun beforeTextChanged(
                s: CharSequence, start: Int,
                count: Int, after: Int
            ) {
            }

            override fun onTextChanged(
                s: CharSequence, start: Int,
                before: Int, count: Int
            ) {
                if (mSelectedAuth != null) {
                    if (amount.text.length > 0) {
                        val dosage =
                            (_v?.findViewById<View>(R.id.activityEdit_spryingAmount) as EditText).text.toString()
                        val d = java.lang.Float.valueOf(dosage)
                        if (d < mSelectedAuth!!.minValue!! || d > mSelectedAuth!!.maxValue!!) {
                            // showErrorMessage("Dose is out of allowed range");
                            recoLabel?.setText(R.string.out_range_dose)
                            recomendedLay?.setBackground(activity!!.getDrawable(R.drawable.reco_red))
                            //     recomendedLay.getBackground().setColorFilter(Color.parseColor("#60da7801"), PorterDuff.Mode.SRC_ATOP);
                        } else {
                            // recomendedLay.getBackground().setColorFilter(Color.parseColor("#603cc367"), PorterDuff.Mode.SRC_ATOP);
                            recomendedLay?.setBackground(activity!!.getDrawable(R.drawable.reco_green))
                            // recomendedLay.setBackgroundColor(Color.parseColor("#603cc367"));
                            recoLabel?.setText(R.string.reco_dose)
                        }
                    } else {
                        //   recomendedLay.getBackground().setColorFilter(Color.parseColor("#603cc367"), PorterDuff.Mode.SRC_ATOP);
                        recomendedLay?.setBackground(activity!!.getDrawable(R.drawable.reco_green))
                        // recomendedLay.setBackgroundColor(Color.parseColor("#603cc367"));
                        recoLabel?.text = "Recommended dose"
                    }
                }
            }
        })
        _v!!.findViewById<View>(R.id.chooseAuthorizedUses)
            .setOnClickListener { //  showErrorMessage("clickkkk");
                dialogg = Dialog(requireActivity(), android.R.style.Theme_Light)
                dialogg!!.requestWindowFeature(Window.FEATURE_NO_TITLE)
                dialogg!!.setContentView(R.layout.dialog_authorized_uses)
                progressDialog = ProgressDialog(activity, R.style.NewDialog)
                progressDialog!!.setCancelable(false)
                progressDialog!!.setCanceledOnTouchOutside(false)
                progressDialog!!.setProgressStyle(android.R.style.Widget_ProgressBar_Large)
                progressDialog!!.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                val dlg_search_btn = dialogg!!.findViewById<Button>(R.id.dlg_search_btn)
                val closeIv = dialogg!!.findViewById<ImageView>(R.id.closeIv)
                progress = dialogg!!.findViewById(R.id.progress)
                progress?.setVisibility(View.GONE)
                authRv = dialogg!!.findViewById(R.id.selectedProctRv)
                authRv?.setLayoutManager(LinearLayoutManager(context))
                mPlagueSelectedAuth = null
                mCropSelectedAuth = ""
                mChemicalSelectedAuth = null
                setAuthPlagues(dialogg!!)
                setAuthCrops(dialogg!!)
                setAuthProducts(dialogg!!)
                closeIv.setOnClickListener { dialogg!!.dismiss() }
                dlg_search_btn.setOnClickListener { v ->
                    dismissKeyboard(v)
                    var pId = ""
                    if (mPlagueSelectedAuth != null) {
                        pId = mPlagueSelectedAuth!!.id!!
                    }
                    var cId = ""
                    if (mChemicalSelectedAuth != null) {
                        cId = mChemicalSelectedAuth!!.id.toString()
                    }
                    progressDialog!!.show()
                    Log.e("mCropSelectedAuth", "--> $mCropSelectedAuth")
                    Log.e("pId", "--> $pId")
                    Log.e("cId", "--> $cId")
                    presenter!!.searchAuthUses(mCropSelectedAuth, pId, cId)
                }
                dialogg!!.show()
            }
        _v!!.findViewById<View>(R.id.activityEdit_overview).setOnClickListener {
            if (mChemicalSelected != null) {
                if (ActivityCompat.checkSelfPermission(
                        mActivity,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                    == PackageManager.PERMISSION_GRANTED
                ) {
                    startLoading()
                    DownloadFile().execute(
                        Util.getOverviewURL(
                            mChemicalSelected!!.register_number
                        ),
                        mChemicalSelected!!.register_number + ".pdf"
                    )
                } else {
                    requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), STORAGE)
                }
            }
        }

        // lv.setItemsCanFocus(false);
        _v!!.findViewById<View>(R.id.dlg_activityEdit_okBtn).setOnClickListener {
            val result = checkData()
            if (result == null) {
                val dosage =
                    (_v?.findViewById<View>(R.id.activityEdit_spryingAmount) as EditText).text.toString()
                val d = java.lang.Float.valueOf(dosage)
                if (mSelectedAuth != null) {
                    if (d < mSelectedAuth!!.minValue!! || d > mSelectedAuth!!.maxValue!!) {
                        showErrorMessage("Dose is out of allowed range")
                    }
                }
                mListener!!.onSaveSprayingDialogClick()
            } else {
                (mActivity as Holder).showError(getString(R.string.general_error_fill_data) + " " + result)
            }
        }
        builder.setView(_v)

        // load spinners and download data
        // justification activitySpray_justificationKeys
        val arrJust = Util.generateSpinnerArray(
            resources.getStringArray(R.array.spraying_justification_values),
            resources.getStringArray(R.array.spraying_justification_labels)
        )
        val spinnerArrayAdapterJust = ArrayAdapter(
            mActivity, android.R.layout.simple_spinner_item, arrJust
        )
        spinnerArrayAdapterJust.setDropDownViewResource(R.layout.row_spinner_item)
        (_v?.findViewById<View>(R.id.activityEdit_spryingJustification) as Spinner).adapter =
            spinnerArrayAdapterJust

        // amount scope
        val arrAmount = Util.generateSpinnerArray(
            resources
                .getStringArray(R.array.activitySpray_amountScopeKeys),
            resources.getStringArray(R.array.activitySpray_amountScope)
        )
        val spinnerArrayAdapterAmScope = ArrayAdapter(
            mActivity, android.R.layout.simple_spinner_item, arrAmount
        )
        spinnerArrayAdapterAmScope.setDropDownViewResource(R.layout.row_spinner_item)
        (_v?.findViewById<View>(R.id.activityEdit_spryingAmountScope) as Spinner).adapter =
            spinnerArrayAdapterAmScope

        // effectiveness
        val arrEffectiveness = Util.generateSpinnerArray(
            resources.getStringArray(R.array.activitySpray_effectivenessKeys),
            resources.getStringArray(R.array.activitySpray_effectiveness)
        )
        val spinnerArrayAdapterEffectiveness = ArrayAdapter(
            mActivity, android.R.layout.simple_spinner_item, arrEffectiveness
        )
        spinnerArrayAdapterEffectiveness.setDropDownViewResource(R.layout.row_spinner_item)
        (_v?.findViewById<View>(R.id.activityEdit_spryingEffectiveness) as Spinner).adapter =
            spinnerArrayAdapterEffectiveness

        // hide keyboard listener
        _v?.setOnClickListener(View.OnClickListener { view -> hideSoftKeyboard(view) })
        _v!!.findViewById<View>(R.id.scroll_spraying)
            .setOnTouchListener(OnTouchListener { view, event ->
                val action = MotionEventCompat.getActionMasked(event)
                if (action != MotionEvent.ACTION_MOVE) {
                    hideSoftKeyboard(view)
                    return@OnTouchListener true
                }
                false
            })
        var d = builder.create()
        d = Util.makeDialogFullScreen(d)
        return d
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }

    override fun onResume() {
        super.onResume()
        presenter!!.onResume()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            STORAGE -> {

                // If request is cancelled, the result arrays are empty.
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startLoading()
                    if (mChemicalSelected!!.register_number != null) {
                        DownloadFile().execute(
                            Util.getOverviewURL(mChemicalSelected!!.register_number),
                            mChemicalSelected!!.register_number + ".pdf"
                        )
                    }
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
            }
        }
    }

    fun hideSoftKeyboard(view: View) {
        val inputMethodManager =
            mActivity.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.rootView.windowToken, 0)
    }

    protected fun checkData(): String? {
        val returnValue: String? = null

        // check the chemical
        if (mChemicalSelected == null) {
            if (treatmentRadioGroup!!.checkedRadioButtonId == R.id.alternativeTreatment) {
            } else {
                _v!!.findViewById<View>(R.id.activityEdit_spryingChemicalsA).setBackgroundColor(
                    Color.RED
                )
                return getString(R.string.field_chemical)
            }
        } else {
            _v!!.findViewById<View>(R.id.activityEdit_spryingChemicalsA)
                .setBackgroundColor(Color.WHITE)
        }

        // check the plague
        val plagueName = (_v!!
            .findViewById<View>(R.id.activityEdit_spryingPlagueA) as AutoCompleteTextView)
            .text
            .toString()

/*        Plague pl = null;
        if (mPlagues == null) {
            _v.findViewById(R.id.activityEdit_spryingPlagueA).setBackgroundColor(Color.RED);
            return getString(R.string.field_plague);
        }
if(mPlagues!=null)
{
        for (Object plague : mPlagues) {
            if (plagueName.equals(((Plague) plague).name)) {
                pl = (Plague) plague;
                break;
            }
        }
        }*/

        if (mPlagueSelected == null) {
            _v!!.findViewById<View>(R.id.activityEdit_spryingPlagueA).setBackgroundColor(Color.RED)
            return getString(R.string.field_plague)
        } else {
            _v!!.findViewById<View>(R.id.activityEdit_spryingPlagueA)
                .setBackgroundColor(Color.WHITE)
            //  mPlagueSelected = pl;
        }
        val dosage =
            (_v!!.findViewById<View>(R.id.activityEdit_spryingAmount) as EditText).text.toString()
        if (dosage == "." || dosage == "") {
            _v!!.findViewById<View>(R.id.activityEdit_spryingAmount).setBackgroundColor(Color.RED)
            return getString(R.string.activityEdit_sprayingsAttributesAmountTxt)
        } else {
            _v!!.findViewById<View>(R.id.activityEdit_spryingAmount).setBackgroundColor(Color.WHITE)
        }
        return returnValue
    }

    override fun startLoading() {
        super.startLoading()
        val v = _v!!.findViewById<View>(R.id.progress)
        v.visibility = View.VISIBLE
    }

    override fun stopLoading() {
        super.stopLoading()
        val v = _v!!.findViewById<View>(R.id.progress)
        v.visibility = View.GONE
    }

    override fun displayPurchases(products: List<Purchase>?) {
        Log.e("oooo", Gson().toJson(products))
        Log.e("oooo size", products!!.size.toString() + "")
        items = ArrayList()
        for (i in products!!.indices) {
            Log.e("iiioooi", "-->$i")
            for (j in products[i].purchaseItems!!.indices) {
                if (products[i].purchaseItems!![j].purchasableType.equals(
                        "Product",
                        ignoreCase = true
                    )
                ) {
                    products[i].purchaseItems!![j].name = products[i].company
                    items!!.add(products[i].purchaseItems!![j])
                }
            }
        }
        _v!!.findViewById<View>(R.id.chooseProductTv).setOnClickListener {
            _v!!.findViewById<View>(R.id.activityEdit_productFromStock).performClick()
        }
        val multiSelectSpinnerWithSearch =
            _v!!.findViewById<MultiSpinnerSearch>(R.id.activityEdit_productFromStock)

        // Pass true If you want searchView above the list. Otherwise false. default = true.
        multiSelectSpinnerWithSearch.isSearchEnabled = true

        // A text that will display in search hint.
        multiSelectSpinnerWithSearch.setSearchHint("Search product")

        // Set text that will display when search result not found...
        multiSelectSpinnerWithSearch.setEmptyTitle("Not Data Found!")

        // If you will set the limit, this button will not display automatically.
        multiSelectSpinnerWithSearch.isShowSelectAllButton = false

        //A text that will display in clear text button
        multiSelectSpinnerWithSearch.setClearText("Close & Clear")
        listArray1 = ArrayList()
        ActivityEditProductDialog.itemTemp = ArrayList()
        for (i in items!!.indices) {
            val keyPairBoolData = KeyPairBoolData()
            keyPairBoolData.id = items!![i].id.toString().toLong()
            keyPairBoolData.name = items!![i].name + "-" + items!![i].lotNumber
            keyPairBoolData.isSelected = false
            keyPairBoolData.setObject(items!![i])
            listArray1!!.add(keyPairBoolData)
        }
        val recyclerView = _v!!.findViewById<RecyclerView>(R.id.selectedProctRv)
        // Removed second parameter, position. Its not required now..
        // If you want to pass preselected items, you can do it while making listArray,
        // Pass true in setSelected of any item that you want to preselect
        multiSelectSpinnerWithSearch.setItems(listArray1) { itemss ->
            ActivityEditProductDialog.itemTemp!!.clear()
            for (i in itemss.indices) {
                if (itemss[i].isSelected) {
                    ActivityEditProductDialog.itemTemp!!.add(items!![i])
                    //   Log.i(TAG, i + " : " + itemss.get(i).getName() + " : " + itemss.get(i).isSelected());
                }
            }
            val adapter = SelectedProductsAdapter(
                requireActivity(),
                ActivityEditProductDialog.itemTemp!!,
                this@ActivityEditSprayingDialog
            )
            recyclerView.adapter = adapter
            recyclerView.layoutManager = LinearLayoutManager(context)
        }
        /*        */
        /** * If you want to set limit as maximum item should be selected is 2. * For No limit -1 or do not call this method. *  */ /*
        multiSelectSpinnerWithSearch.setLimit(2, new MultiSpinnerSearch.LimitExceedListener() {
            @Override
            public void onLimitListener(KeyPairBoolData data) {
                Toast.makeText(getActivity(), "Limit exceed ", Toast.LENGTH_LONG).show();
            }
        });*/Log.e("ddd size--", items!!.size.toString() + "")
        Log.e("ddd", Gson().toJson(items))
    }

    private fun setAuthProducts(dialogg: Dialog) {
        val mDropdown = dialogg.findViewById<AutoCompleteTextView>(R.id.productSearchInput)
        if (mChemicals != null) {
            val spinnerArrayAdapter = ChemicalSpinnerAdapter(
                mActivity, R.layout.row_spinner_item, mChemicals!!
            )
            spinnerArrayAdapter.setDropDownViewResource(R.layout.row_spinner_item)
            mDropdown.setAdapter(spinnerArrayAdapter)
        }
        mDropdown.onItemClickListener =
            AdapterView.OnItemClickListener { parent, view, position, rowId ->
                mChemicalSelectedAuth = parent.getItemAtPosition(position) as Chemical
                Log.e("ccc", Gson().toJson(mChemicalSelectedAuth))
                mDropdown.dismissDropDown()
                mDropdown.clearFocus()
                dismissKeyboard(_v)
            }
        mDropdown.onFocusChangeListener = OnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                mDropdown.setText("")
                mChemicalSelectedAuth = null
            }
        }
    }

    private fun setAuthCrops(dialogg: Dialog) {
        // set spinner values
        val spinnerArrayAdapter = ArrayAdapter(
            mActivity, R.layout.row_spinner_item, MainActivity.mAuthorizedCropsList
        )
        spinnerArrayAdapter.setDropDownViewResource(R.layout.row_spinner_item)
        val mDropdown = dialogg.findViewById<AutoCompleteTextView>(R.id.cropSearchInput)
        mDropdown.setAdapter(spinnerArrayAdapter)
        mDropdown.onItemClickListener =
            AdapterView.OnItemClickListener { parent, view, position, rowId ->
                mDropdown.dismissDropDown()
                mDropdown.clearFocus()
                mCropSelectedAuth = parent.adapter.getItem(position).toString()
                Log.e("ccc", Gson().toJson(mCropSelectedAuth))
                dismissKeyboard(_v)
            }
        mDropdown.onFocusChangeListener = OnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                mDropdown.setText("")
                mCropSelectedAuth = ""
            }
        }
    }

    private fun setAuthPlagues(dialogg: Dialog) {


        // set spinner values
        val strArray = arrayOfNulls<String>(mPlagues!!.size)
        if (mPlagues != null) {
            for (i in mPlagues!!.indices) {
                strArray[i] = mPlagues!![i].name
            }
        }
        val spinnerArrayAdapter = ArrayAdapter(
            mActivity, R.layout.row_spinner_item, strArray
        )
        spinnerArrayAdapter.setDropDownViewResource(R.layout.row_spinner_item)
        val mDropdown = dialogg.findViewById<AutoCompleteTextView>(R.id.plagueSearchInput)
        mDropdown.setAdapter(spinnerArrayAdapter)
        mDropdown.onItemClickListener =
            AdapterView.OnItemClickListener { parent, view, position, rowId ->
                mDropdown.dismissDropDown()
                mDropdown.clearFocus()
                for (i in mPlagues!!.indices) {
                    if (parent.adapter.getItem(position).toString()
                            .equals(mPlagues!![i].name, ignoreCase = true)
                    ) {
                        mPlagueSelectedAuth = mPlagues!![i]
                        break
                    }
                    //strArray[i] = mPlagues.get(i).name;
                }
                Log.e("ff", Gson().toJson(mPlagueSelectedAuth))
                dismissKeyboard(_v)
            }
        mDropdown.onFocusChangeListener = OnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                mDropdown.setText("")
                mPlagueSelectedAuth = null
            }
        }
    }

    override fun fillDropdownChemicals(chemicals: List<Chemical>?) {
        mChemicals = chemicals
        val mDropdown = _v!!.findViewById<AutoCompleteTextView>(R.id.activityEdit_spryingChemicalsA)
        mDropdown.setOnClickListener {
            mDropdown.clearFocus()
            mDropdown.requestFocus()
        }
        val strArray = arrayOfNulls<String>(chemicals!!.size)
        if (chemicals != null) {
            for (i in chemicals.indices) {
                strArray[i] = chemicals[i].name
            }
        }
        val spinnerArrayAdapter = ArrayAdapter(
            mActivity, R.layout.row_spinner_item, strArray
        )
        if (mChemicals != null) {
           /* val spinnerArrayAdapter = ChemicalSpinnerAdapter(
                mActivity, R.layout.row_spinner_item, strArray!!
            )*/
            spinnerArrayAdapter.setDropDownViewResource(R.layout.row_spinner_item)
            mDropdown.setAdapter(spinnerArrayAdapter)
        }
        mDropdown.onItemClickListener =
            AdapterView.OnItemClickListener { parent, view, position, rowId ->
                Log.e("ccc", Gson().toJson(parent.getItemAtPosition(position)))
for (i in 0..chemicals.size)
{
    if(chemicals.get(i).name!!.equals(parent.getItemAtPosition(position)))
    {
        mChemicalSelected = chemicals.get(i)
        break

    }

}

              //  mChemicalSelected = chemicals.get(position)
                Log.e("ccc", Gson().toJson(mChemicalSelected))
                _v!!.findViewById<View>(R.id.activityEdit_overview).visibility = View.VISIBLE
                mDropdown.dismissDropDown()
                mDropdown.clearFocus()
                dismissKeyboard(_v)
            }

        /*  mDropdown.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    mDropdown.setText("");
                    mChemicalSelected = null;
                }
            }
        });*/
    }

    override fun fillDropdownPlagues(plagues: List<Plague>?) {
        mPlagues = plagues

        // set spinner values
        val strArray = arrayOfNulls<String>(plagues!!.size)
        if (plagues != null) {
            for (i in plagues.indices) {
                strArray[i] = plagues[i].name
            }
        }
        val spinnerArrayAdapter = ArrayAdapter(
            mActivity, R.layout.row_spinner_item, strArray
        )
        spinnerArrayAdapter.setDropDownViewResource(R.layout.row_spinner_item)
        val mDropdown = _v!!.findViewById<AutoCompleteTextView>(R.id.activityEdit_spryingPlagueA)
        mDropdown.setAdapter(spinnerArrayAdapter)
        mDropdown.setOnClickListener {
            mDropdown.clearFocus()
            mDropdown.requestFocus()
        }
        mDropdown.onItemClickListener =
            AdapterView.OnItemClickListener { parent, view, position, rowId ->
                mDropdown.dismissDropDown()
                mDropdown.clearFocus()
                mPlagueSelected = plagues[position]
                dismissKeyboard(_v)
            }
    }

    override fun authUsesList(authUsesList: List<AuthorizedUse>?) {
        progressDialog!!.dismiss()
        if (authUsesList!!.size == 0) {
            showErrorMessage("No data found")
        }
        val adapter = AuthUsesAdapter(requireActivity(),
            authUsesList as MutableList<AuthorizedUse>, this)
        authRv!!.adapter = adapter
        authRv!!.layoutManager = LinearLayoutManager(context)
    }

    val sprayingAttribute: SprayingAttribute
        get() {
            val strAtt = SprayingAttribute()
            if (treatmentRadioGroup!!.checkedRadioButtonId == R.id.alternativeTreatment) {
                strAtt.alternative_treatment = true
                strAtt.alternative_product = activityEdit_alternativeProduct!!.text.toString()
                strAtt.alternative_product_units =
                    (_v!!.findViewById<View>(R.id.unitsValue) as EditText).text.toString()
                strAtt.chemical_id = ""
                strAtt.chemical_desc = ""
                strAtt.amount_scope = ""
            } else {
                strAtt.alternative_treatment = false
                strAtt.alternative_product = ""
                strAtt.alternative_product_units = ""
                strAtt.chemical_id = mChemicalSelected!!.id
                strAtt.chemical_desc = mChemicalSelected!!.name
                strAtt.amount_scope =
                    ((_v!!.findViewById<View>(R.id.activityEdit_spryingAmountScope) as Spinner)
                        .selectedItem as DropdownItem)
                        .value
            }
            strAtt.plague_id = mPlagueSelected!!.id
            strAtt.plague_desc = mPlagueSelected!!.name
            strAtt.plantings_purchase_items_attributes = ActivityEditProductDialog.itemTemp
            strAtt.lot_number = (_v!!.findViewById<View>(R.id.activityEdit_lotNumber) as EditText)
                .text
                .toString()
            strAtt.justification = ((_v!!
                .findViewById<View>(R.id.activityEdit_spryingJustification) as Spinner)
                .selectedItem as DropdownItem)
                .value
            strAtt.amount = (_v!!.findViewById<View>(R.id.activityEdit_spryingAmount) as EditText)
                .text
                .toString()
            strAtt.security_time =
                (_v!!.findViewById<View>(R.id.activityEdit_spryingSecurityTime) as EditText)
                    .text
                    .toString()
            strAtt.effectiveness =
                ((_v!!.findViewById<View>(R.id.activityEdit_spryingEffectiveness) as Spinner)
                    .selectedItem as DropdownItem)
                    .value
            return strAtt
        }

    private inner class DownloadFile : AsyncTask<String?, Void?, Long>() {
        private var fileName: String? = null


        override fun onPostExecute(result: Long) {
            if (!isAdded) {
                return
            }
            if (result != 1L || fileName == null) {
                Util.showToast(mActivity, R.string.error_fetching_data)
                stopLoading()
                return
            }
            val pdfFile = File(
                Environment.getExternalStorageDirectory().toString() + "/Agricolum/" + fileName
            )
            val pdfIntent = Intent(Intent.ACTION_VIEW)
            pdfIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            try {
                val path: Uri
                if (Build.VERSION.SDK_INT > VERSION_CODES.M) {
                    // find file path usign content provider
                    path = FileProvider.getUriForFile(context!!, "com.agricolum.provider", pdfFile)
                    pdfIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } else {
                    // find the path from the file
                    path = Uri.fromFile(pdfFile)
                }
                pdfIntent.setDataAndType(path, "application/pdf")

                // try to open pdf file
                startActivity(pdfIntent)
                stopLoading()
            } catch (e: ActivityNotFoundException) {
                Log.e(TAG, "Error reading pdf file", e)
                stopLoading()
                Util.showToast(mActivity, getString(R.string.pdf_error))
            } catch (e: Exception) {
                Log.e(TAG, "Error reading pdf file", e)
                stopLoading()
                Util.showToast(mActivity, getString(R.string.pdf_error))
            }
        }

        override fun doInBackground(vararg params: String?): Long {
            val fileUrl = params[0]
            fileName = params[1]
            val extStorageDirectory = Environment.getExternalStorageDirectory().toString()
            val folder = File(extStorageDirectory, "Agricolum")
            folder.mkdir()
            val pdfFile = File(folder, fileName)
            return try {
                pdfFile.createNewFile()
                FileDownloader.downloadFile(fileUrl, pdfFile)
                1L
            } catch (e: IOException) {
                Log.e("ACTIVITY", "Error downloading PDF", e)
                0L
            } catch (e: Exception) {
                e.printStackTrace()
                0L
            }
        }
    }

    companion object {
        private const val TAG = "APP/SPRAYING"
        private const val STORAGE = 78
    }
}