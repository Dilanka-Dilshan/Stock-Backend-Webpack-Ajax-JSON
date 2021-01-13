
/*
 * @author : Dilanka Dilshan<ehd.dilanka@gmail.com>
 */

package lk.dila.mode;

public class Item {
    private String code;
    private String description;
    private int qty;
    private String unitPrice;

    public Item() {
    }

    public Item(String code, String description, int qty, String unitPrice) {
        this.code = code;
        this.description = description;
        this.qty = qty;
        this.unitPrice = unitPrice;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getQty() {
        return qty;
    }

    public void setQty(int qty) {
        this.qty = qty;
    }

    public String getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(String unitPrice) {
        this.unitPrice = unitPrice;
    }
}
