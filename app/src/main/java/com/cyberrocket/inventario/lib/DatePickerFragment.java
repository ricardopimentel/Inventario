package com.cyberrocket.inventario.lib;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.DatePicker;
import android.widget.TextView;

import androidx.fragment.app.DialogFragment;

import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

public class DatePickerFragment extends DialogFragment implements DatePickerDialog.OnDateSetListener, DatePickerDialog.OnCancelListener{
    private final TextView previsaoDevolucao;

    public DatePickerFragment(TextView previsaoDevolucao) {
        this.previsaoDevolucao = previsaoDevolucao;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the current date as the default date in the picker
        Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        // Create a new instance of DatePickerDialog and return it
        DatePickerDialog datePickerDialog = new DatePickerDialog(getActivity(), this, year, month, day);

        Calendar cMax = Calendar.getInstance();
        Calendar cMin = Calendar.getInstance();
        cMax.add(Calendar.DATE, 15);
        datePickerDialog.getDatePicker().setMaxDate(cMax.getTimeInMillis());
        datePickerDialog.getDatePicker().setMinDate(cMin.getTimeInMillis());

        List<Calendar> daysList = new LinkedList<>();
        Calendar[] daysArray;
        Calendar cAux = Calendar.getInstance();

        while(cAux.getTimeInMillis() <= cMax.getTimeInMillis()){
            if(cAux.get(Calendar.DAY_OF_WEEK) != 1 && cAux.get(Calendar.DAY_OF_WEEK) != 7){
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(cAux.getTimeInMillis());
                daysList.add(calendar);
            }
            cAux.setTimeInMillis(cAux.getTimeInMillis()+(24*60*60*1000));
        }
        daysArray = new Calendar[daysList.size()];
        for(int i = 0; i < daysArray.length; i++){
            daysArray[i] = daysList.get(i);
        }
        //datePickerDialog.setSelectableDays(daysArray);
        return datePickerDialog;
    }

    @Override
    public void onDateSet(DatePicker view, int year, int month, int day) {
        previsaoDevolucao.setText(year+"-"+(month+1)+"-"+day);
        showTimePickerDialog(view);
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        previsaoDevolucao.setText("");
    }

    public void showTimePickerDialog(View v) {
        DialogFragment newFragment = new TimePickerFragment(previsaoDevolucao);
        newFragment.show(getActivity().getSupportFragmentManager(), "timePicker");
    }
}
