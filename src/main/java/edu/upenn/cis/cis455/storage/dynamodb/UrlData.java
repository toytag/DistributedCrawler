package edu.upenn.cis.cis455.storage.dynamodb;

import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBIndexHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;

@DynamoDBTable(tableName=DynamoDBInterface.URL_TABLE)
public class UrlData implements Serializable {
    
    private String url; 
    private String md5;
    private Date date = new Date(System.currentTimeMillis());
    private Set<String> outboundLinks = new HashSet<>();
    private Float weight = 0f;

    @DynamoDBHashKey(attributeName="url")
    public String getUrl() {
        return url;
    }
    
    public void setUrl(String url) {
        this.url = url;
    }
    
    @DynamoDBIndexHashKey(globalSecondaryIndexName="md5-index", attributeName="md5")
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

    @DynamoDBAttribute(attributeName="outboundLinks")
    public Set<String> getOutboundLinks() {
        return outboundLinks;
    }
    
    public void setOutboundLinks(Set<String> outboundLinks) {
        this.outboundLinks = outboundLinks;
    }
    
    @DynamoDBAttribute(attributeName="weight")
    public Float getWeight() {
        return weight;
    }
    
    public void setWeight(Float weight) {
        this.weight = weight;
    }

}
