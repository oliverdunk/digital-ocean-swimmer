package com.yassirh.digitalocean.service;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.app.NotificationCompat;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.yassirh.digitalocean.R;
import com.yassirh.digitalocean.data.DatabaseHelper;
import com.yassirh.digitalocean.data.SSHKeyDao;
import com.yassirh.digitalocean.model.Account;
import com.yassirh.digitalocean.model.SSHKey;
import com.yassirh.digitalocean.utils.ApiHelper;

public class SSHKeyService {

	private Context context;
	private boolean isRefreshing;
		
	public SSHKeyService(Context context) {
		this.context = context;
	}

	public void getAllSSHKeysFromAPI(final boolean showProgress){
		Account currentAccount = ApiHelper.getCurrentAccount(context);
		if(currentAccount == null){
			return;
		}
		isRefreshing = true;
		String url = String.format("%s/account/keys/", ApiHelper.API_URL);//String url = "https://api.digitalocean.com/ssh_keys/?client_id=" + currentAccount.getClientId() + "&api_key=" + currentAccount.getApiKey();
		AsyncHttpClient client = new AsyncHttpClient();
		client.addHeader("Authorization", String.format("Bearer %s", currentAccount.getToken()));
		client.get(url, new AsyncHttpResponseHandler() {
			NotificationManager mNotifyManager;
			NotificationCompat.Builder mBuilder;
			
			@Override
			public void onStart() {
				if(showProgress){
					mNotifyManager =
					        (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
					mBuilder = new NotificationCompat.Builder(context);
					mBuilder.setContentTitle(context.getResources().getString(R.string.synchronising))
					    .setContentText(context.getResources().getString(R.string.synchronising_keys))
					    .setSmallIcon(R.drawable.ic_launcher);
					mBuilder.setContentIntent(PendingIntent.getActivity(context,0,new Intent(),PendingIntent.FLAG_UPDATE_CURRENT));
					mNotifyManager.notify(NotificationsIndexes.NOTIFICATION_GET_ALL_KEYS, mBuilder.build());
				}
			}
			
			@Override
			public void onFinish() {
				isRefreshing = false;
				if(showProgress){
					mNotifyManager.cancel(NotificationsIndexes.NOTIFICATION_GET_ALL_KEYS);
				}
			}
			
			@Override
			public void onProgress(int bytesWritten, int totalSize) {
				if(showProgress){
					mBuilder.setProgress(100, (int)100*bytesWritten/totalSize, false);
					mNotifyManager.notify(NotificationsIndexes.NOTIFICATION_GET_ALL_KEYS, mBuilder.build());
				}
			}
			
			@Override
			public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
				if(statusCode == 401){
					ApiHelper.showAccessDenied();
				}
			}
			
		    @Override
		    public void onSuccess(String response) {
		        try {
					JSONObject jsonObject = new JSONObject(response);
					List<SSHKey> sshKeys = new ArrayList<SSHKey>();
					JSONArray sshKeysJSONArray = jsonObject.getJSONArray("ssh_keys");
					for(int i = 0; i < sshKeysJSONArray.length(); i++){
						JSONObject sshKeysJSONObject = sshKeysJSONArray.getJSONObject(i);
						SSHKey sshKey = new SSHKey();
						sshKey.setId(sshKeysJSONObject.getLong("id"));
						sshKey.setName(sshKeysJSONObject.getString("name"));
						sshKey.setFingerprint(sshKeysJSONObject.getString("fingerprint"));
						sshKey.setPublicKey(sshKeysJSONObject.getString("public_key"));
						sshKeys.add(sshKey);
					}
					SSHKeyService.this.deleteAll();
					SSHKeyService.this.saveAll(sshKeys);
					SSHKeyService.this.setRequiresRefresh(true);
				} catch (JSONException e) {
					e.printStackTrace();
				}  
		    }

		});
	}
	
	protected void saveAll(List<SSHKey> sshKeys) {
		SSHKeyDao sshKeyDao = new SSHKeyDao(DatabaseHelper.getInstance(context));
		for (SSHKey sshKey : sshKeys) {
			sshKeyDao.create(sshKey);
		}
	}
	
	public void deleteAll() {
		SSHKeyDao sshKeyDao = new SSHKeyDao(DatabaseHelper.getInstance(context));
		sshKeyDao.deleteAll();
	}
	
	public List<SSHKey> getAllSSHKeys(){
		SSHKeyDao sshKeyDao = new SSHKeyDao(DatabaseHelper.getInstance(context));
		return sshKeyDao.getAll(null);
	}

	public void setRequiresRefresh(Boolean requireRefresh){
		SharedPreferences settings = context.getSharedPreferences("prefrences", 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putBoolean("key_require_refresh", requireRefresh);
		editor.commit();
	}
	public Boolean requiresRefresh(){
		SharedPreferences settings = context.getSharedPreferences("prefrences", 0);
		return settings.getBoolean("key_require_refresh", true);
	}
	
	public SSHKey findById(long id) {
		SSHKeyDao sshKeyDao = new SSHKeyDao(DatabaseHelper.getInstance(context));
		return sshKeyDao.findById(id);
	}

	public void delete(final long id, final boolean showProgress) {
		Account currentAccount = ApiHelper.getCurrentAccount(context);
		if(currentAccount == null){
			return;
		}
		String url = "";//"https://api.digitalocean.com/ssh_keys/" + id + "/destroy/" + "?client_id=" + currentAccount.getClientId() + "&api_key=" + currentAccount.getApiKey();
		
		AsyncHttpClient client = new AsyncHttpClient();
		client.get(url, new AsyncHttpResponseHandler() {
			NotificationManager mNotifyManager;
			NotificationCompat.Builder mBuilder;
			
			@Override
			public void onStart() {
				if(showProgress){
					mNotifyManager =
					        (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
					mBuilder = new NotificationCompat.Builder(context);
					mBuilder.setContentTitle(context.getResources().getString(R.string.destroying_ssh_key))
					    .setContentText("")
					    .setSmallIcon(R.drawable.ic_launcher);

					mNotifyManager.notify(NotificationsIndexes.NOTIFICATION_DESTROY_SSH_KEY, mBuilder.build());
				}
			}
			
		    @Override
		    public void onSuccess(String response) {
		        try {
					JSONObject jsonObject = new JSONObject(response);
					String status = jsonObject.getString("status");
					if(ApiHelper.API_STATUS_OK.equals(status)){
						
					}
					else{
						// TODO handle error Access Denied/Not Found
					}
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}  
		    }
		    
		    @Override
			public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
				if(statusCode == 401){
					ApiHelper.showAccessDenied();
				}
			}
		    
		    @Override
			public void onProgress(int bytesWritten, int totalSize) {	
				if(showProgress){
					mBuilder.setProgress(100, (int)100*bytesWritten/totalSize, false);
					mNotifyManager.notify(NotificationsIndexes.NOTIFICATION_DESTROY_SSH_KEY, mBuilder.build());
				}
			}
		    
		    @Override
		    public void onFinish() {
		    	if(showProgress)
					mNotifyManager.cancel(NotificationsIndexes.NOTIFICATION_DESTROY_SSH_KEY);
		    	SSHKeyDao sshKeyDao = new SSHKeyDao(DatabaseHelper.getInstance(context));
		    	sshKeyDao.delete(id);
		    	SSHKeyService.this.setRequiresRefresh(true);
		    }
		});
	}

	public void save(SSHKey sshKey, final boolean update, final boolean showProgress) {
		Account currentAccount = ApiHelper.getCurrentAccount(context);
		if(currentAccount == null){
			return;
		}
		String url = "";
		if(update){
			/*url = "https://api.digitalocean.com/ssh_keys/" + sshKey.getId() + "/edit/" + 
					"?client_id=" + currentAccount.getClientId() + 
					"&api_key=" + currentAccount.getApiKey() +
					"&ssh_pub_key=" + sshKey.getSshPubKey() +
					"&name=" + sshKey.getName();*/
		} else{
			/*url = "https://api.digitalocean.com/ssh_keys/new/" + 
					"?client_id=" + currentAccount.getClientId() + 
					"&api_key=" + currentAccount.getApiKey() +
					"&ssh_pub_key=" + sshKey.getSshPubKey() +
					"&name=" + sshKey.getName();*/
		}
		AsyncHttpClient client = new AsyncHttpClient();
		client.get(url, new AsyncHttpResponseHandler() {
			NotificationManager mNotifyManager;
			NotificationCompat.Builder mBuilder;
			
			@Override
			public void onStart() {
				if(showProgress){
					mNotifyManager =
					        (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
					mBuilder = new NotificationCompat.Builder(context);
					mBuilder.setContentTitle(context.getResources().getString(R.string.synchronising))
					    .setContentText(context.getResources().getString(R.string.saving_ssh_key))
					    .setSmallIcon(R.drawable.ic_launcher);
					mBuilder.setContentIntent(PendingIntent.getActivity(context,0,new Intent(),PendingIntent.FLAG_UPDATE_CURRENT));
					if(update)
						mNotifyManager.notify(NotificationsIndexes.NOTIFICATION_UPDATE_SSH_KEY, mBuilder.build());
					else
						mNotifyManager.notify(NotificationsIndexes.NOTIFICATION_CREATE_SSH_KEY, mBuilder.build());
				}
			}
			
			@Override
			public void onFinish() {
				if(showProgress && update)
					mNotifyManager.cancel(NotificationsIndexes.NOTIFICATION_UPDATE_SSH_KEY);
				else if(showProgress && !update)
					mNotifyManager.cancel(NotificationsIndexes.NOTIFICATION_CREATE_SSH_KEY);
			}
			
			@Override
			public void onProgress(int bytesWritten, int totalSize) {
				if(showProgress){
					mBuilder.setProgress(100, (int)100*bytesWritten/totalSize, false);
					if(update)
						mNotifyManager.notify(NotificationsIndexes.NOTIFICATION_UPDATE_SSH_KEY, mBuilder.build());
					else
						mNotifyManager.notify(NotificationsIndexes.NOTIFICATION_CREATE_SSH_KEY, mBuilder.build());
				}
			}
			
			@Override
			public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
				if(statusCode == 401){
					ApiHelper.showAccessDenied();
				}
			}
			
		    @Override
		    public void onSuccess(String response) {
		        try {
					JSONObject jsonObject = new JSONObject(response);
					String status = jsonObject.getString("status");
					if(ApiHelper.API_STATUS_OK.equals(status)){
						JSONObject sshKeysJSONObject = jsonObject.getJSONObject("ssh_key");
						SSHKey sshKey = new SSHKey();
						sshKey.setId(sshKeysJSONObject.getLong("id"));
						sshKey.setName(sshKeysJSONObject.getString("name"));
						sshKey.setPublicKey(sshKeysJSONObject.getString("ssh_pub_key"));
						SSHKeyDao sshKeyDao = new SSHKeyDao(DatabaseHelper.getInstance(context));
						sshKeyDao.create(sshKey);
				    	SSHKeyService.this.setRequiresRefresh(true);
					}
				} catch (JSONException e) {
					e.printStackTrace();
				}  
		    }

		});
	}

	public boolean isRefreshing() {
		return isRefreshing;
	}
}