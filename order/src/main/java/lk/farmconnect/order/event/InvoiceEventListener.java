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
    private final StorageService storageService; // Changed from FileStorageService
    private final OrderRepository orderRepository;

    @Async
    @EventListener
    @Transactional
    public void handleOrderAccepted(OrderAcceptedEvent event) {
        Order order = event.getOrder();

        try {
            log.info("Generating invoice for order {} asynchronously...", order.getOrderNumber());

            // Generate PDF bytes
            byte[] pdfBytes = invoiceService.generateInvoice(order);

            // Wrap bytes in our production-safe MultipartFile
            ByteArrayMultipartFile pdfFile = new ByteArrayMultipartFile(
                    pdfBytes,
                    "Invoice_" + order.getOrderNumber() + ".pdf",
                    "application/pdf"
            );

            // Upload using StorageService with DOCUMENT type
            String invoiceKey = storageService.uploadFile(pdfFile, "invoices", StorageService.FileType.DOCUMENT);

            // Generate presigned URL for the invoice
            String invoiceUrl = storageService.getPresignedUrl(invoiceKey);

            // Save URL to Database
            order.setInvoiceUrl(invoiceUrl);
            orderRepository.save(order);

            log.info("Invoice successfully uploaded to S3 for order {}", order.getOrderNumber());

        } catch (Exception e) {
            log.error("Failed to generate/upload invoice for order {}", order.getOrderNumber(), e);
        }
    }
}