package com.nt202.knockvpn;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link KnockingPortsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class KnockingPortsFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private SharedPreferences prefs;

    private ListView listView;
    private ArrayList<PortSequence> sequences = new ArrayList<>();

    public KnockingPortsFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment KnockingPortsFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static KnockingPortsFragment newInstance(String param1, String param2) {
        KnockingPortsFragment fragment = new KnockingPortsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_knocking_ports, container, false);
    }





    private void loadSavedSequences() {
        String json = prefs.getString("sequences", "");
        if (!json.isEmpty()) {
            Type type = new TypeToken<ArrayList<PortSequence>>(){}.getType();
            sequences = new Gson().fromJson(json, type);
        }
        updateList();
    }

    private void saveSequences() {
        SharedPreferences.Editor editor = prefs.edit();
        String json = new Gson().toJson(sequences);
        editor.putString("sequences", json);
        editor.apply();
    }

    private void updateList() {
        listView.setAdapter(new SequenceAdapter(this.getContext(), R.layout.list_item, sequences));
        saveSequences();
    }

    private void showAddSequenceDialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this.getContext());
        builder.setTitle("Add New Sequence");

        final View dialogView = LayoutInflater.from(this.getContext()).inflate(R.layout.dialog_add, null);
        final EditText etHost = (EditText) dialogView.findViewById(R.id.etHost);
        final LinearLayout portContainer = (LinearLayout) dialogView.findViewById(R.id.portContainer);
        final Button btnAddPort = (Button) dialogView.findViewById(R.id.btnAddPort);

        final ArrayList<PortWithProtocol> tempSequence = new ArrayList<>();

        btnAddPort.setOnClickListener(v -> addPortRow(portContainer, tempSequence));

        builder.setView(dialogView);
        builder.setPositiveButton("Save", (dialog, which) -> {
            String host = etHost.getText().toString();
            if (!host.isEmpty() && !tempSequence.isEmpty()) {
                sequences.add(new PortSequence(host, new ArrayList<>(tempSequence)));
                updateList();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void addPortRow(LinearLayout container, final ArrayList<PortWithProtocol> sequence) {
        View row = LayoutInflater.from(this.getContext()).inflate(R.layout.port_row, null);
        final EditText etPort = (EditText) row.findViewById(R.id.etPort);
        final Spinner spProtocol = (Spinner) row.findViewById(R.id.spProtocol);
        Button btnRemove = (Button) row.findViewById(R.id.btnRemove);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this.getContext(),
                R.array.protocols, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spProtocol.setAdapter(adapter);

        btnRemove.setOnClickListener(v -> {
            container.removeView(row);
            sequence.remove(sequence.size() - 1);
        });

        container.addView(row);
        sequence.add(new PortWithProtocol(0, Protocol.TCP)); // Placeholder

        etPort.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                try {
                    int port = Integer.parseInt(s.toString());
                    PortWithProtocol pwp = sequence.get(sequence.size() - 1);
                    pwp.setPort(port);
                    pwp.setProtocol(Protocol.values()[spProtocol.getSelectedItemPosition()]);
                } catch (NumberFormatException e) {
                    // Invalid port number
                }
            }
            // Other TextWatcher methods
        });
    }

    class SequenceAdapter extends ArrayAdapter<PortSequence> {
        public SequenceAdapter(Context context, int resource, List<PortSequence> objects) {
            super(context, resource, objects);
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item, parent, false);
                holder = new ViewHolder();
                holder.tvHost = (TextView) convertView.findViewById(R.id.tvHost);
                holder.btnOpen = (Button) convertView.findViewById(R.id.btnOpen);
                holder.btnClose = (Button) convertView.findViewById(R.id.btnClose);
                holder.btnDelete = (android.widget.ImageButton) convertView.findViewById(R.id.btnDelete); // Add this line
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            final PortSequence seq = getItem(position);
            holder.tvHost.setText(seq.getHost());

            StringBuilder ports = new StringBuilder();
            for (PortWithProtocol pwp : seq.getSequence()) {
                ports.append(pwp.getPort()).append(" (").append(pwp.getProtocol()).append(") → ");
            }

            holder.btnOpen.setOnClickListener(v -> PortKnocker.knock(seq.getHost(), seq.getSequence()));

            holder.btnClose.setOnClickListener(v -> {
                ArrayList<PortWithProtocol> reversed = new ArrayList<>(seq.getSequence());
                Collections.reverse(reversed);
                PortKnocker.knock(seq.getHost(), reversed);
            });

            holder.btnDelete.setOnClickListener(v -> new AlertDialog.Builder(getContext())
                    .setTitle("Confirm Delete")
                    .setMessage("Delete configuration for " + seq.getHost() + "?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        sequences.remove(position);
                        notifyDataSetChanged();
                        saveSequences();
                    })
                    .setNegativeButton("Cancel", null)
                    .show());

            return convertView;
        }

        class ViewHolder {
            TextView tvHost;
            Button btnOpen;
            Button btnClose;
            ImageButton btnDelete;
        }
    }
}