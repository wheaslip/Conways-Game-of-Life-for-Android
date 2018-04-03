package com.heslihop.wesley.gameoflife;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import java.util.ArrayList;
import java.util.List;

public class SavedStateList extends AppCompatActivity implements MyMediatorInterface, RecyclerItemTouchHelper.RecyclerItemTouchHelperListener{

    private List<SavedState> stateList = new ArrayList<>();
    private RecyclerView recyclerView;
    private SavedStateAdapter ssAdapter;
    private DatabaseHelper dbh;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_saved_state_list);

        dbh = new DatabaseHelper(this);

        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);

        stateList = dbh.updateStateList();

        ssAdapter = new SavedStateAdapter(stateList, this, dbh);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(ssAdapter);

        ItemTouchHelper.SimpleCallback itemTouchHelperCallback = new RecyclerItemTouchHelper(0, ItemTouchHelper.LEFT, this);
        new ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(recyclerView);
    }

    @Override
    public void userItemClick (int pos) {
        //Toast.makeText(SavedStateList.this, "Clicked: " + stateList.get(pos).getName(), Toast.LENGTH_SHORT).show();
        dbh.getData(stateList.get(pos).getId());
        finish();
    }

    // Removes item when swiped to the left.
    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction, int position) {
        if (viewHolder instanceof SavedStateAdapter.MyViewHolder) {
            ssAdapter.removeItem(viewHolder.getAdapterPosition());
        }
    }


}
