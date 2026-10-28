package vn.edu.bookstore.legacy.adapter.soap;

import jakarta.xml.soap.SOAPException;
import jakarta.xml.ws.handler.MessageContext;
import jakarta.xml.ws.handler.soap.SOAPHandler;
import jakarta.xml.ws.handler.soap.SOAPMessageContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.namespace.QName;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Set;

public class SoapLoggingHandler implements SOAPHandler<SOAPMessageContext> {

    private static final Logger LOG = LoggerFactory.getLogger(SoapLoggingHandler.class);

    @Override
    public Set<QName> getHeaders() {
        return Collections.emptySet();
    }

    @Override
    public boolean handleMessage(SOAPMessageContext context) {
        log(context, false);
        return true;
    }

    @Override
    public boolean handleFault(SOAPMessageContext context) {
        log(context, true);
        return true;
    }

    @Override
    public void close(MessageContext context) {
    }

    private void log(SOAPMessageContext context, boolean fault) {
        boolean outbound = Boolean.TRUE.equals(context.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY));
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            context.getMessage().writeTo(out);
            LOG.info("SOAP {}{}:\n{}",
                    outbound ? " gửi" : " nhận",
                    fault ? " (Fault)" : "",
                    out.toString(StandardCharsets.UTF_8));
        } catch (SOAPException | IOException e) {
            LOG.warn("Không ghi được log thông điệp SOAP: {}", e.getMessage());
        }
    }
}
