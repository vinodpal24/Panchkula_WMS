package com.wms.panchkula.Validation

import android.app.Activity
import android.content.Context
import android.view.View
import android.widget.EditText
import android.widget.Toast
import com.google.android.material.textfield.TextInputEditText
import java.util.regex.Matcher
import java.util.regex.Pattern

class Validation {
    lateinit var view: View
    lateinit var activity:Activity
    lateinit var textInputLayout: TextInputEditText
    lateinit var mobile: String


    fun isValidEmailCheck(activity: Activity , email:String): Boolean {
        val pattern: Pattern
        val EMAIL_PATTERN = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+"
        pattern = Pattern.compile(EMAIL_PATTERN)
        val matcher: Matcher = pattern.matcher(email)
        return matcher.matches()
    }

    fun isValidEmail(activity:Activity, textInputLayout: TextInputEditText, email:String): Boolean {
        val pattern: Pattern
        val matcher: Matcher
        val EMAIL_PATTERN = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+"
        pattern = Pattern.compile(EMAIL_PATTERN)
        matcher = pattern.matcher(email)
        return if(!matcher.matches())
        {
            textInputLayout.error = "Please enter correct mail Id"
            false
        }
        else if(email.isNullOrEmpty())
        {
            textInputLayout.error = "Field can not be empty"
            false
        }
        else
        {
            textInputLayout.error = null
            true
        }
    }

    fun isValidEmailEdit(activity:Activity, editText: EditText, email:String): Boolean {
        val pattern: Pattern
        val matcher: Matcher
        val EMAIL_PATTERN = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+"
        pattern = Pattern.compile(EMAIL_PATTERN)
        matcher = pattern.matcher(email)
        return if(!matcher.matches())
        {
            editText.error = "Please enter correct mail Id"
            false
        }
        else if(email.isNullOrEmpty())
        {
            editText.error = "Field can not be empty"
            false
        }
        else
        {
            editText.error = null
            true
        }
    }

    fun isCheckNull(activity: Activity, textInputLayout: EditText, text:String):Boolean {
        return if(text.isEmpty())
        { textInputLayout.error = "Field should not be empty"
            false }
        else
        { textInputLayout.error = null
            true }
    }

    fun isValidIp(ip: String): Boolean {
        return android.util.Patterns.IP_ADDRESS.matcher(ip).matches()
    }

    fun checkMobileLength(activity: Activity, textInputLayout: TextInputEditText, text: String): Boolean{
        this.activity = activity
        return if(text.length < 10)
        {
            textInputLayout.setError("Invalid Number, Please Enter 10 Digit.")
            false
        }
        else if(text.length > 10){
            textInputLayout.setError("Invalid Number!")
            false
        }
        else{
            textInputLayout.error = null
            true
        }
    }

    fun checkMobileLength(activity: Activity, editText: EditText, text: String): Boolean{
        this.activity = activity
        return if(text.length < 10)
        {
            editText.setError("Invalid Number, Please Enter 10 Digit.")
            false
        }
        else if(text.length > 10){
            editText.setError("Invalid Number!")
            false
        }
        else{
            editText.error = null
            true
        }
    }

    fun isCheckUserId(activity: Activity, textInputLayout: EditText, text:String):Boolean {
        return if(text.isEmpty())
        { textInputLayout.error = "Please Select User id"
            false }
        else
        { textInputLayout.error = null
            true }
    }

    fun validatePassword(activity:Activity, textInputLayout: TextInputEditText, pass:String): Boolean {
        val pattern: Pattern
        val pattern1: Pattern
        val pattern2: Pattern

        val PASSWORD_PATTERN = "^(?=.*[0-9])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,}$"
        val minPassLength = ".{8,}$"
        val oneCapital = "^(?=.*[A-Z]).$"

        pattern = Pattern.compile(PASSWORD_PATTERN)
        pattern1 = Pattern.compile(minPassLength)
        val matcher: Matcher = pattern.matcher(pass)
        val matcher1: Matcher = pattern1.matcher(pass)
        return if (pass.isEmpty()) {
            textInputLayout.error = "Field can not be empty"
            false
        }
        else if(!matcher1.matches()){
            textInputLayout.error = "Password Length should have min 8 characters"
            false
        }
        else if (!matcher.matches()) {
            textInputLayout.error = "Enter valid Password"
            false
        }
        else if (!matcher.matches()) {
            textInputLayout.error = "Enter valid Password"
            false
        }
        else {
            textInputLayout.error = null
            true
        }
    }

    fun  isPassMatch(context: Context, text1: EditText, text2: String):Boolean{
        return if(text1.text.toString() != text2){
            Toast.makeText(context,"Password does not match", Toast.LENGTH_SHORT).show()
            false
        } else
            true
    }

    fun  isAccNoMatch(context: Context, text1: EditText, text2: String):Boolean{
        return if(text1.text.toString() != text2){
            Toast.makeText(context, "Account no. does not match", Toast.LENGTH_SHORT).show()
            false
        } else
            true
    }

//    fun isImage(activity: Activity, arrIamge:ArrayList<Product_Model.Images>):Boolean{
//        return if(arrIamge.isEmpty()){
//            Toast.makeText(activity,"Please set the image", Toast.LENGTH_SHORT).show()
//            false
//        } else
//            true
//    }

    fun isSingleImage(activity: Activity,url:String):Boolean{
        return if(url.isNullOrEmpty()){
            Toast.makeText(activity,"Please set the image", Toast.LENGTH_SHORT).show()
            false
        } else
            true
    }

    fun categoryValidation(activity: Activity, textInputLayout: EditText, text:String):Boolean{
        return if(text.equals("Category"))
        {             Toast.makeText(activity,"Please set the category", Toast.LENGTH_SHORT).show()
            false }
        else
        { true
        }
    }

    fun checkdivisible(activity: Activity,textInputLayout: EditText,int: String):Boolean{
        return if(!(int.toInt()%30 == 0))
        {
            textInputLayout.error = "Should be in multiple of 30's"
            false
        }else
        {
            textInputLayout.error = null
            true
        }
    }

}