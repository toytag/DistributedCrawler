package edu.upenn.cis.cis455.storage.dynamodb;

import java.io.Serializable;
import java.util.Date;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;

@DynamoDBTable(tableName=DynamoDBInterface.DOCUMENT_TABLE)
public class DocumentData implements Serializable {

    private String md5;
    private Date date = new Date(System.currentTimeMillis());
    private String document; 
    
    @DynamoDBHashKey(attributeName="md5")
    public String getMd5() {
        return md5;
    }
    
    public void setMd5(String md5) {
        this.md5 = md5;
    }

    @DynamoDBAttribute(attributeName="date")
    public Date getDate() {
        return date;
    }
    
    public void setDate(Date date) {
        this.date = date;
    }
    
    @DynamoDBAttribute(attributeName="document")
    public String getDocument() {
        return document;
    }
    
    public void setDocument(String document) {
        this.document = document;
    }

}
