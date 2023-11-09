package com.agricolum.ui.fragment

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import com.agricolum.ui.fragment.BaseDialogFragment
import com.agricolum.ui.contracts.ChooseProductContracts
import com.agricolum.interfaces.OnAdapterItemClick
import com.agricolum.domain.model.PurchasesModel.PurchaseItem
import com.androidbuts.multispinnerfilter.KeyPairBoolData
import com.agricolum.domain.model.DropdownItem
import com.agricolum.ui.fragment.ActivityEditProductDialog.NoticeProductDialogListener
import com.agricolum.ui.fragment.ActivityEditProductDialog
import com.agricolum.R
import android.widget.EditText
import android.os.Bundle
import android.util.Log
import com.agricolum.ui.presenter.ChooseProductPresenter
import android.widget.TextView
import android.view.WindowManager
import com.google.gson.Gson
import android.widget.ArrayAdapter
import android.widget.Spinner
import org.json.JSONObject
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.AutoCompleteTextView
import com.agricolum.ui.adapter.ProductSpinnerAdapter
import android.widget.AdapterView
import com.agricolum.domain.model.PurchasesModel.Purchase
import com.androidbuts.multispinnerfilter.MultiSpinnerSearch
import androidx.recyclerview.widget.RecyclerView
import com.agricolum.adapter.SelectedProductsAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import com.agricolum.Util
import com.agricolum.domain.model.Product
import com.agricolum.domain.model.ProductAttr
import com.agricolum.domain.model.Seed
import com.agricolum.interfaces.Holder
import com.agricolum.ui.activities.MainActivity
import java.lang.Exception
import java.util.ArrayList
import kotlin.Throws

class ActivityEditProductDialog : BaseDialogFragment(), ChooseProductContracts.View,
    OnAdapterItemClick {
    var items: ArrayList<PurchaseItem>? = null
    var listArray1: ArrayList<KeyPairBoolData>? = null
    var _v: View? = null
    var mProducts: List<Product>? = null
    var seeds: Array<DropdownItem?>? = null
    var new_product = false
    private var mProductSelected: Product? = null
    private var presenter: ChooseProductContracts.Presenter? = null

    // Use this instance of the interface to deliver action events
    var mListener: NoticeProductDialogListener? = null
    private var mAactivityType = 0
    override fun onQuantityDataChange() {
        var tempQuantity = 0
        // Log.e("rere", new Gson().toJson(itemTemp));
        for (i in itemTemp!!.indices) {
            if (itemTemp!![i].quantity_selected != null) {
                if (itemTemp!![i].quantity_selected != "") {
                    tempQuantity = tempQuantity + itemTemp!![i].quantity_selected!!.toInt()
                }
            }
        }
        (_v!!.findViewById<View>(R.id.activityEdit_productQuantity) as EditText).setText(
            tempQuantity.toString()
        )
    }

    /*
     * The activity that creates an instance of this dialog fragment must
     * implement this interface in order to receive event callbacks. Each method
     * passes the DialogFragment in case the host needs to query it.
     */
    interface NoticeProductDialogListener {
        fun onSaveProductDialogClick()
    }

    fun setListener(listener: NoticeProductDialogListener?) {
        mListener = listener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        presenter = ChooseProductPresenter(context, this)
        presenter?.fetchPurchases(true)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(activity)

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        _v = requireActivity().layoutInflater.inflate(R.layout.dialog_activityedit_product, null)

        // set title
        (_v?.findViewById<View>(R.id.dlg_activityEdit_title) as TextView).text =
            getString(R.string.activityEdit_productsTitleTxt)
        _v!!.findViewById<View>(R.id.dlg_activityEdit_okBtn).setOnClickListener {
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            requireActivity().window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
            if (new_product) {
                createNewProduct()
            } else {
                Log.e("iteee", Gson().toJson(itemTemp))
                mListener!!.onSaveProductDialogClick()
            }
        }
        builder.setView(_v)

        //load spinners and download data units
        val arrUnits = Util.generateSpinnerArray(
            resources.getStringArray(R.array.activityProduct_unitsKeys),
            resources.getStringArray(R.array.activityProduct_units)
        )
        val spinnerArrayAdapter = ArrayAdapter(
            mActivity, android.R.layout.simple_spinner_item, arrUnits
        )
        spinnerArrayAdapter.setDropDownViewResource(R.layout.row_spinner_item)
        (_v?.findViewById<View>(R.id.activityEdit_productUnits) as Spinner).adapter =
            spinnerArrayAdapter
        _v!!.findViewById<View>(R.id.new_product).setOnClickListener {
            new_product = !new_product
            if (new_product) {
                showNewProductPanel()
                (_v?.findViewById<View>(R.id.dlg_activityEdit_okBtn) as TextView).text =
                    getString(R.string.create)
            } else {
                hideNewProductPanel()
            }
        }
        return builder.create()
    }

    private fun createNewProduct() {
        try {
            val payload = createProductJson()
            Log.e("fdfdf", Gson().toJson(payload))
            Log.e("iteee", Gson().toJson(itemTemp))
            if (payload != null) {
                presenter!!.createProduct(payload)
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        presenter!!.onResume()
        return view
    }

    override fun onDestroyView() {
        presenter!!.onDestroy()
        super.onDestroyView()
    }

    override fun displayProducts(products: List<Product>?) {
        mProducts = products
        if (mProducts!!.isEmpty()) {
            _v!!.findViewById<View>(R.id.activityEdit_no_products).visibility = View.VISIBLE
        } else {
            _v!!.findViewById<View>(R.id.activityEdit_no_products).visibility = View.GONE
        }
        val mDropdown = _v!!.findViewById<AutoCompleteTextView>(R.id.activityEdit_product)
        mDropdown.setOnClickListener {
            mDropdown.clearFocus()
            mDropdown.requestFocus()
        }
        if (mProducts != null) {
            // set spinner values
            val adapter = ProductSpinnerAdapter(mActivity, R.layout.row_spinner_item, mProducts)
            adapter.setDropDownViewResource(R.layout.row_spinner_item)
            mDropdown.setAdapter(adapter)
        }
        mDropdown.onItemClickListener =
            AdapterView.OnItemClickListener { parent, view, position, rowId -> // remove invalid color
                mDropdown.setBackgroundColor(Color.WHITE)
                // set selected product
                mProductSelected = parent.getItemAtPosition(position) as Product
                mDropdown.dismissDropDown()
                mDropdown.clearFocus()
                dismissKeyboard(_v)
            }
    }

    override fun displayPurchases(products: List<Purchase>?) {
        items = ArrayList()
        for (i in products!!.indices) {
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
        itemTemp = ArrayList()
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
            itemTemp!!.clear()
            for (i in itemss.indices) {
                if (itemss[i].isSelected) {
                    itemTemp!!.add(items!![i])
                    Log.i(TAG, i.toString() + " : " + itemss[i].name + " : " + itemss[i].isSelected)
                }
            }
            val adapter =
                SelectedProductsAdapter(requireActivity(), itemTemp!!, this@ActivityEditProductDialog)
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
        });*/
    }

    override fun fillDropdownSeeds(seeds: List<Seed>?) {
        // set spinner values
        val values = arrayOfNulls<DropdownItem>(seeds!!.size)
        if (seeds != null) {
            for (i in seeds.indices) {
                values[i] = DropdownItem(seeds[i].id, seeds[i].translated_name)
            }
        }
        this.seeds = values
        val spinnerArrayAdapterEffectiveness = ArrayAdapter(
            mActivity, android.R.layout.simple_spinner_item, values
        )
        spinnerArrayAdapterEffectiveness.setDropDownViewResource(R.layout.row_spinner_item)
        (_v!!.findViewById<View>(R.id.product_seed) as Spinner).adapter =
            spinnerArrayAdapterEffectiveness
    }

    override fun selectProduct(product: Product?) {
        val position = mProducts!!.indexOf(product)
        if (position != -1) {
            // remove invalid color
            _v!!.findViewById<View>(R.id.activityEdit_product).setBackgroundColor(Color.WHITE)
            // set selected product
            mProductSelected = mProducts!![position]
            (_v!!.findViewById<View>(R.id.activityEdit_product) as AutoCompleteTextView).setText(
                mProductSelected!!.name
            )
        }
    }

    override fun showNewProductPanel() {
        new_product = true
        _v!!.findViewById<View>(R.id.new_product_layout).visibility = View.VISIBLE
        _v!!.findViewById<View>(R.id.product_layout).visibility = View.GONE
    }

    override fun hideNewProductPanel() {
        new_product = false
        _v!!.findViewById<View>(R.id.new_product_layout).visibility =
            View.GONE
        _v!!.findViewById<View>(R.id.product_layout).visibility = View.VISIBLE
        // reset form
        (_v!!.findViewById<View>(R.id.activityEdit_productQuantity) as EditText).setText("")
        (_v!!.findViewById<View>(R.id.product_seed) as Spinner).setSelection(-1)
    }

    override fun showInfoMessage(message: String?) {
        (mActivity as Holder).showInfo(message)
    }

    override fun showErrorMessage(message: String?) {
        (mActivity as Holder).showError(message)
    }

    override fun startLoading() {
        _v!!.findViewById<View>(R.id.progress).visibility = View.VISIBLE
    }

    override fun stopLoading() {
        _v!!.findViewById<View>(R.id.progress).visibility = View.GONE
    }

    fun showError(error: Int) {
        if (mActivity == null) {
            return
        }
        (mActivity as Holder).showError(error)
    }// productAttribute.product_id = itemTemp.get(0).name;
    /*   } else {
      // show error
      int colorId = getResources().getColor(R.color.red_wrong);
      _v.findViewById(R.id.activityEdit_product).setBackgroundColor(colorId);
  }
  return null; *///   if (mProductSelected != null) {

    // remove invalid color
// show error
    // remove invalid color
    val productAttribute: ProductAttr?
        get() {
            val productAttribute = ProductAttr()
            return if (itemTemp != null && itemTemp!!.size == 0) {
                if (mProductSelected != null) {

                    // remove invalid color
                    _v!!.findViewById<View>(R.id.activityEdit_product)
                        .setBackgroundColor(Color.WHITE)
                    productAttribute.quantity =
                        (_v!!.findViewById<View>(R.id.activityEdit_productQuantity) as EditText).text.toString()
                    if (productAttribute.quantity == "" || productAttribute.quantity == ".") {
                        _v!!.findViewById<View>(R.id.activityEdit_productQuantity)
                            .setBackgroundColor(Color.RED)
                        return null
                    }
                    val product: Product = mProductSelected as Product
                    productAttribute.product_id = product.product_id
                    productAttribute.variety = product.variety
                    productAttribute.name = product.name
                    productAttribute.plantings_purchase_items_attributes = itemTemp
                    productAttribute.units =
                        ((_v!!.findViewById<View>(R.id.activityEdit_productUnits) as Spinner)
                            .selectedItem as DropdownItem)
                            .value
                    return productAttribute
                } else {
                    // show error
                    val colorId = resources.getColor(R.color.red_wrong)
                    _v!!.findViewById<View>(R.id.activityEdit_product).setBackgroundColor(colorId)
                }
                null
            } else {
                //   if (mProductSelected != null) {

                // remove invalid color
                _v!!.findViewById<View>(R.id.activityEdit_product).setBackgroundColor(Color.WHITE)
                productAttribute.quantity =
                    (_v!!.findViewById<View>(R.id.activityEdit_productQuantity) as EditText).text.toString()
                if (productAttribute.quantity == "" || productAttribute.quantity == ".") {
                    _v!!.findViewById<View>(R.id.activityEdit_productQuantity)
                        .setBackgroundColor(Color.RED)
                    return null
                }
                if (mProductSelected != null) {
                    val product: Product = mProductSelected as Product
                    productAttribute.product_id = product.product_id
                    productAttribute.variety = product.variety
                    productAttribute.name = product.name
                } else {
                    productAttribute.product_id = ""
                    productAttribute.variety = ""
                    // productAttribute.product_id = itemTemp.get(0).name;
                    productAttribute.name = itemTemp!![0].name
                }
                productAttribute.plantings_purchase_items_attributes = itemTemp
                productAttribute.units =
                    ((_v!!.findViewById<View>(R.id.activityEdit_productUnits) as Spinner)
                        .selectedItem as DropdownItem)
                        .value
                productAttribute
                /*   } else {
                  // show error
                  int colorId = getResources().getColor(R.color.red_wrong);
                  _v.findViewById(R.id.activityEdit_product).setBackgroundColor(colorId);
              }
              return null; */
            }
        }

    fun setActivityType(aActivityType: Int) {
        mAactivityType = aActivityType
    }

    override fun onDestroy() {
        super.onDestroy()
        (mActivity as MainActivity).showingDialog = false
    }

    @Throws(Exception::class)
    private fun createProductJson(): JSONObject {
        val json = JSONObject()
        val variety = (_v!!.findViewById<View>(R.id.product_variety) as TextView).text.toString()
        val invalidColorId = resources.getColor(R.color.red_wrong)
        if (variety == "") {
            _v!!.findViewById<View>(R.id.product_variety).setBackgroundColor(invalidColorId)
            throw Exception()
        } else {
            json.put("variety", variety)
            _v!!.findViewById<View>(R.id.product_variety).setBackgroundColor(Color.WHITE)
        }
        val selectedSeed = (_v!!.findViewById<View>(R.id.product_seed) as Spinner)
            .selectedItem as DropdownItem
        if (selectedSeed == null) {
            _v!!.findViewById<View>(R.id.product_seed).setBackgroundColor(invalidColorId)
            throw Exception()
        } else {
            val seed_id = selectedSeed.value
            val seed_name = selectedSeed.text
            json.put("seed_id", seed_id)
            json.put("seed_name", seed_name)
            _v!!.findViewById<View>(R.id.product_seed).setBackgroundColor(Color.WHITE)
        }
        val product = JSONObject()
        product.put("product", json)
        return product
    }

    companion object {
        private const val TAG = "APP/PRODUCTS"
        @JvmField
        var itemTemp: ArrayList<PurchaseItem>? = null
    }
}