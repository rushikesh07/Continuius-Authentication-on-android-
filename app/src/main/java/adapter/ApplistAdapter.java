package adapter;


import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import com.sng.bacgroundcameratutorial.MyLib;
import com.sng.bacgroundcameratutorial.R;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import models.AppModel;
import repository.mSqiteHelper;
import services.CameraService;

public class ApplistAdapter extends RecyclerView.Adapter<ApplistAdapter.MyViewHolder> {

    List<AppModel> appModels=new ArrayList<>();
    Context context;
    mSqiteHelper mSqiteHelper;
    AlertDialog.Builder builder;
    public ApplistAdapter(List<AppModel> appModels, Context context)
    {
        this.appModels=appModels;
        this.context=context;
        builder=new AlertDialog.Builder(context);
        mSqiteHelper=new mSqiteHelper(context);
    }

    public class MyViewHolder extends RecyclerView.ViewHolder{
        TextView tv_appname;
        Switch switch_lock;
        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            tv_appname=(TextView)itemView.findViewById(R.id.tv_appname);
            switch_lock=(Switch)itemView.findViewById(R.id.switch_lock);
        }
    }
    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view= LayoutInflater.from(context).inflate(R.layout.layout_apps,parent,false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final MyViewHolder holder, int position) {
      final AppModel model=appModels.get(position);
      holder.tv_appname.setText(model.getApp_name());
      if(mSqiteHelper.isAppPresent(model.getApp_package_name())==1)
      holder.switch_lock.setChecked(true);
      else
      holder.switch_lock.setChecked(false);
      holder.switch_lock.setOnCheckedChangeListener(new OnSwitchChangeListener(model,holder));
    }

    @Override
    public int getItemCount() {
        return appModels.size();
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }


   private class OnSwitchChangeListener implements CompoundButton.OnCheckedChangeListener{
        AppModel model;
        MyViewHolder viewHolder;
        public OnSwitchChangeListener(AppModel model,MyViewHolder viewHolder)
        {
            this.model=model;
            this.viewHolder=viewHolder;
        }
       @Override
       public void onCheckedChanged(CompoundButton buttonView, final boolean isChecked) {
           if(MyLib.isServiceRunning(context, CameraService.class)) {
               if (isChecked) {
                   ContentValues contentValues = new ContentValues();
                   contentValues.put(mSqiteHelper.APP_PACKAGE_NAME, model.getApp_package_name());
                   mSqiteHelper.insert(contentValues);
               }
               if (!isChecked) {
                   ContentValues contentValues = new ContentValues();
                   mSqiteHelper.delete(model.getApp_package_name());
               }
           }
           else
           {
               builder.setMessage("Not allowed while CAS is stoped.");
               builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                   @Override
                   public void onClick(DialogInterface dialogInterface, int i) {
                       dialogInterface.dismiss();
                       if(isChecked) {
                           viewHolder.switch_lock.setOnCheckedChangeListener(null);
                           viewHolder.switch_lock.setChecked(false);
                           viewHolder.switch_lock.setOnCheckedChangeListener(new OnSwitchChangeListener(model,viewHolder));
                       }
                       if(!isChecked) {
                           viewHolder.switch_lock.setOnCheckedChangeListener(null);
                           viewHolder.switch_lock.setChecked(true);
                           viewHolder.switch_lock.setOnCheckedChangeListener(new OnSwitchChangeListener(model,viewHolder));
                       }
                   }
               });
               builder.show();
           }

       }
   }
}
