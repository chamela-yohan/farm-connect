package lk.farmconnect.order.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lk.farmconnect.order.entity.Order;
import lk.farmconnect.order.entity.OrderItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
public class InvoiceService {

    // Generate the PDF and return it as a byte array
    public byte[] generateInvoice(Order order) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 40, 40, 40, 40);
            PdfWriter.getInstance(document, out);
            document.open();

            // Header
            Font titleFont = new Font(Font.HELVETICA, 24, Font.BOLD, new java.awt.Color(41, 128, 185));
            Paragraph title = new Paragraph("FARMCONNECT INVOICE", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            // Order Details
            Font normalFont = new Font(Font.HELVETICA, 10, Font.NORMAL);
            Font boldFont = new Font(Font.HELVETICA, 10, Font.BOLD);

            document.add(new Paragraph("Order Number: " + order.getOrderNumber(), boldFont));
            document.add(new Paragraph("Date: " + order.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")), normalFont));
            document.add(new Paragraph("Status: " + order.getStatus().toString(), normalFont));
            document.add(Chunk.NEWLINE);

            // Buyer & Farmer Info
            PdfPTable infoTable = new PdfPTable(2);
            infoTable.setWidthPercentage(100);
            infoTable.addCell(createCell("BILLED TO:", boldFont));
            infoTable.addCell(createCell("FULFILLED BY:", boldFont));
            infoTable.addCell(createCell(order.getBuyer().getName() + "\n" + order.getBuyer().getEmail(), normalFont));
            infoTable.addCell(createCell(order.getFarmer().getName() + "\n" + order.getFarmer().getEmail(), normalFont));
            document.add(infoTable);
            document.add(Chunk.NEWLINE);

            // Items Table
            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setWidths(new int[]{3, 1, 2, 2});

            // Headers
            table.addCell(createHeaderCell("Product"));
            table.addCell(createHeaderCell("Qty"));
            table.addCell(createHeaderCell("Unit Price"));
            table.addCell(createHeaderCell("Subtotal"));

            // Rows
            for (OrderItem item : order.getItems()) {
                table.addCell(createCell(item.getProductTitleSnapshot(), normalFont));
                table.addCell(createCell(item.getApprovedQty().toPlainString(), normalFont));
                table.addCell(createCell("Rs. " + item.getUnitPriceSnapshot().toPlainString(), normalFont));
                table.addCell(createCell("Rs. " + item.getSubtotal().toPlainString(), normalFont));
            }

            document.add(table);
            document.add(Chunk.NEWLINE);

            // Total
            Font totalFont = new Font(Font.HELVETICA, 12, Font.BOLD);
            Paragraph total = new Paragraph("TOTAL: Rs. " + order.getTotalAmount().toPlainString(), totalFont);
            total.setAlignment(Element.ALIGN_RIGHT);
            document.add(total);

            // Footer / Notes
            document.add(Chunk.NEWLINE);
            if (order.getFarmerNotes() != null) {
                document.add(new Paragraph("Farmer Note: " + order.getFarmerNotes(), new Font(Font.HELVETICA, 9, Font.ITALIC)));
            }
            document.add(new Paragraph("Thank you for supporting local farmers!", new Font(Font.HELVETICA, 9, Font.ITALIC, java.awt.Color.GRAY)));

            document.close();
            return out.toByteArray();

        } catch (Exception e) {
            log.error("Failed to generate PDF invoice for order {}", order.getOrderNumber(), e);
            throw new lk.farmconnect.common.exception.BusinessException("Failed to generate invoice PDF.");
        }
    }

    // Helper methods for table cells
    private PdfPCell createCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(5);
        cell.setBorder(Rectangle.NO_BORDER);
        return cell;
    }

    private PdfPCell createHeaderCell(String text) {
        Font headerFont = new Font(Font.HELVETICA, 10, Font.BOLD, java.awt.Color.WHITE);
        PdfPCell cell = new PdfPCell(new Phrase(text, headerFont));
        cell.setBackgroundColor(new java.awt.Color(41, 128, 185));
        cell.setPadding(5);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        return cell;
    }
}