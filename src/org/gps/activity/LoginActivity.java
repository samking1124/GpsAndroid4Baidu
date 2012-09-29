package org.gps.activity;

import org.gps.db.GpsDBOpenHelper;
import org.gps.service.HttpClientUtil;
import org.gps.service.LoginBean;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

public class LoginActivity extends Activity {
	private EditText usernameEt;
	private EditText passwordEt;
	private CheckBox rememberCb;
	private ProgressDialog pd;
	private Handler handler = new Handler();
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.login);
		usernameEt = (EditText) this.findViewById(R.id.username);
		passwordEt = (EditText) this.findViewById(R.id.password);
		rememberCb = (CheckBox) this.findViewById(R.id.remember);
		LoginBean loginBean = GpsDBOpenHelper.getInstance(this).getLogin();
		if (loginBean != null && loginBean.getRemember() == 1) {
			usernameEt.setText(loginBean.getUsername());
			passwordEt.setText(loginBean.getPassword());
			rememberCb.setChecked(true);
		}
		//定义不确定进度条
		pd = new ProgressDialog(this);
		pd.setMessage("登录中...");
		Button loginBt = (Button) this.findViewById(R.id.loginBt);
		loginBt.setOnClickListener(new LoginListener());
		Button exitBt = (Button) this.findViewById(R.id.exitBt);
		exitBt.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});
		
	}

	private class LoginListener implements OnClickListener {
		@Override
		public void onClick(View v) {
			final String username = usernameEt.getText().toString();
			final String password = passwordEt.getText().toString();
			final boolean isRemember = rememberCb.isChecked();
			if ("".equals(username.trim())) {
				Toast.makeText(LoginActivity.this, "请输入用户名", 3000).show();
				return;
			}
			if ("".equals(password.trim())) {
				Toast.makeText(LoginActivity.this, "请输入密码", 3000).show();
				return;
			}
			pd.show();
			new Thread() {
				@Override
				public void run() {
					
					if (HttpClientUtil.login(username, password)) {
						GpsDBOpenHelper.getInstance(LoginActivity.this)
								.updateLogin(username, password, isRemember);
						LoginActivity.this.setProgressBarIndeterminate(false);
						Intent intent = new Intent(LoginActivity.this,
								MonitorActivity.class);
						startActivity(intent);
						pd.cancel();
						finish();
					} else {
						handler.post(new Runnable() {
							@Override
							public void run() {
								pd.cancel();
								Toast.makeText(LoginActivity.this,
										"用户名或者密码不正确", 3000).show();
							}
						});
					}
				}

			}.start();

		}

	}

}
