package org.pskink.patchworkdrawable;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class Main extends Activity implements OnItemClickListener {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		ListView lv = new ListView(this);
		ArrayAdapter<String> a = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
		a.add("android");
		a.add("flag");
		lv.setAdapter(a);
		lv.setOnItemClickListener(this);
		setContentView(lv);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		String item = (String) parent.getAdapter().getItem(position);
		Intent intent = new Intent(this, Show.class);;
		intent.putExtra("name", item);
		startActivity(intent);
	}
	
}
