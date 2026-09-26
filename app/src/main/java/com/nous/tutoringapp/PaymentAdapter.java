package com.nous.tutoringapp; // Προσοχή: Βεβαιώσου ότι αυτό ταιριάζει με το δικό σου package

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.Locale;

public class PaymentAdapter extends RecyclerView.Adapter<PaymentAdapter.PaymentViewHolder> {

    private ArrayList<Payment> paymentList;

    // Κατασκευαστής: Δέχεται τη λίστα με τις πληρωμές
    public PaymentAdapter(ArrayList<Payment> paymentList) {
        this.paymentList = paymentList;
    }

    // 1. Εδώ το Android παίρνει το XML που φτιάξαμε (item_payment_row) και το "φουσκώνει" (inflate)
    @NonNull
    @Override
    public PaymentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_payment_row, parent, false);
        return new PaymentViewHolder(view);
    }

    // 2. Εδώ βάζουμε τα πραγματικά δεδομένα (ονόματα, ποσά) πάνω στην κάρτα
    @Override
    public void onBindViewHolder(@NonNull PaymentViewHolder holder, int position) {
        Payment currentPayment = paymentList.get(position);

        // Όνομα
        String name = currentPayment.getStudentName();
        holder.tvStudentName.setText(name != null ? name : "Άγνωστος");

        // Αιτιολογία και Τρόπος Πληρωμής
        String details = (currentPayment.getTargetMonth() != null && !currentPayment.getTargetMonth().isEmpty())
                ? currentPayment.getTargetMonth() : currentPayment.getComments();
        holder.tvPaymentDetails.setText(details + " - " + currentPayment.getPaymentMethod());

        // Ποσό
        holder.tvPaymentAmount.setText(String.format(Locale.getDefault(), "%.2f €", currentPayment.getAmount()));

        // 🎯 Βάζουμε ΚΑΙ απλό κλικ (Tap)
        holder.itemView.setOnClickListener(v -> showOptionsDialog(v.getContext(), currentPayment));

        // 🎯 Βάζουμε ΚΑΙ παρατεταμένο κλικ (Long Press)
        holder.itemView.setOnLongClickListener(v -> {
            showOptionsDialog(v.getContext(), currentPayment);
            return true;
        });
    }

    // 🛠️ Φτιάχνουμε μια ξεχωριστή μέθοδο για το παραθυράκι, για να μην γράφουμε διπλό κώδικα!
    private void showOptionsDialog(android.content.Context context, Payment payment) {
        CharSequence[] options = new CharSequence[]{"🖨️ Εκτύπωση Απόδειξης", "❌ Ακύρωση"};

        new android.app.AlertDialog.Builder(context)
                .setTitle("Επιλογές Πληρωμής (" + payment.getAmount() + " €)")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        // 🖨️ Στέλνουμε το αντικείμενο στον εκτυπωτή μας!
                        ReceiptPrinter.printReceipt(context, payment);
                    }
                })
                .show();
    }

    // 3. Πόσα αντικείμενα έχει η λίστα;
    @Override
    public int getItemCount() {
        return paymentList != null ? paymentList.size() : 0;
    }

    // Αυτή η κλάση απλά "κρατάει" τα Textviews για να μην τα ψάχνει συνέχεια το Android (εξοικονόμηση μνήμης)
    static class PaymentViewHolder extends RecyclerView.ViewHolder {
        TextView tvStudentName, tvPaymentDetails, tvPaymentAmount;

        public PaymentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvStudentName = itemView.findViewById(R.id.tvStudentNameRow);
            tvPaymentDetails = itemView.findViewById(R.id.tvPaymentDetailsRow);
            tvPaymentAmount = itemView.findViewById(R.id.tvPaymentAmountRow);
        }
    }
}