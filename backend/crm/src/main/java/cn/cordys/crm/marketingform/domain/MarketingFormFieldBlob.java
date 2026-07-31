package cn.cordys.crm.marketingform.domain;

import cn.cordys.common.domain.BaseResourceField;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Table(name = "marketing_form_field_blob")
public class MarketingFormFieldBlob extends BaseResourceField {
}
