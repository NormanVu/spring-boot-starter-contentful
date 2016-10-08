package com.tomalancarroll.service;

import com.contentful.java.cma.CMACallback;
import com.contentful.java.cma.CMAClient;
import com.contentful.java.cma.model.CMAArray;
import com.contentful.java.cma.model.CMAContentType;
import com.contentful.java.cma.model.CMAField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import static com.contentful.java.cma.Constants.CMAFieldType.Object;
import static com.tomalancarroll.service.ContentfulConstants.TRANSLATION_CONTENT_TYPE_NAME;

@Service
public class ContentfulInitializerService {
    private static final Logger logger = LoggerFactory.getLogger(ContentfulInitializerService.class);

    @Value("${contentful.space.id}")
    private String contentfulSpaceId;

    @Autowired
    private CMAClient contentfulManagementClient;

    public void initialize() {
        try {
            if (!contentTypeIsSetup()) {
                setupContentType();
            } else {
                logger.info("Translation type already exists; skipping intialization");
            }
        } catch (SecurityException e) {
            throw new IllegalStateException("Unable to initialize Contentful. " +
                    "Please check the application properties contentful.space.id and " +
                    "contentful.management.token", e);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to initialize Contentful", e);
        }
    }

    public boolean contentTypeIsSetup() {
        try {
            CMAArray<CMAContentType> result = contentfulManagementClient.contentTypes().fetchAll(contentfulSpaceId);

            for (CMAContentType contentType : result.getItems()) {
                if (TRANSLATION_CONTENT_TYPE_NAME.getValue().equals(contentType.getName())) {
                    return true;
                }
            }
        } catch (Exception e) {
            throw new SecurityException(e);
        }

        return false;
    }

    public void setupContentType() {
        contentfulManagementClient.contentTypes()
                .async()
                .create(contentfulSpaceId,
                        new CMAContentType().setName("Translation")
                                .addField(new CMAField()
                                        .setId("dictionary")
                                        .setName("Dictionary")
                                        .setType(Object)
                                        .setRequired(true)),
                        new CMACallback<CMAContentType>() {
                            @Override
                            protected void onSuccess(CMAContentType result) {
                                logger.info("Successfully created Translation content type");

                                // Content Type starts as draft, we must publish
                                publishContentType();
                            }
                        });
    }

    public void publishContentType() {
        CMAContentType translationType = contentfulManagementClient
                .contentTypes()
                .fetchOne(contentfulSpaceId, TRANSLATION_CONTENT_TYPE_NAME.getValue());

        contentfulManagementClient.contentTypes()
                .publish(translationType);
        logger.info("Successfully published Translation content type");

    }
}