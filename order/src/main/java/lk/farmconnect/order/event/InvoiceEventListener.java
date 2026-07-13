package lk.farmconnect.order.event;

import lk.farmconnect.common.service.StorageService;
import lk.farmconnect.common.util.ByteArrayMultipartFile;
import lk.farmconnect.order.entity.Order;
import lk.farmconnect.order.repository.OrderRepository;
import lk.farmconnect.order.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class InvoiceEventListener {

    private final InvoiceService invoiceService;
    private final StorageService storageService;
    private final OrderRepository orderRepository;

    @Async
    @EventListener
    @Transactional
    public void handleOrderAccepted(OrderAcceptedEvent event) {
        Order order = event.getOrder();

        try {
            log.info("Generating invoice for order {} asynchronously...", order.getOrderNumber());

            // 1. Generate PDF bytes
            byte[] pdfBytes = invoiceService.generateInvoice(order);

            // 2. Wrap bytes in MultipartFile
            ByteArrayMultipartFile pdfFile = new ByteArrayMultipartFile(
                    pdfBytes,
                    "Invoice_" + order.getOrderNumber() + ".pdf",
                    "application/pdf"
            );

            // 3. Upload to MinIO. This returns the KEY (e.g., "invoices/uuid.pdf")
            String invoiceKey = storageService.uploadFile(pdfFile, "invoices", StorageService.FileType.DOCUMENT);

            log.info("Invoice uploaded successfully. Key to be saved: {}", invoiceKey);

            // 4. CRITICAL: Save ONLY the key to the database.
            // DO NOT call storageService.getPresignedUrl() here!
            order.setInvoiceKey(invoiceKey);
            orderRepository.save(order);

            log.info("Invoice key successfully saved to database for order {}", order.getOrderNumber());

        } catch (Exception e) {
            log.error("Failed to generate/upload invoice for order {}", order.getOrderNumber(), e);
        }
    }
}