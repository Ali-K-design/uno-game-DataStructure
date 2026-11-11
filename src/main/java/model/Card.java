package model;

public class Card {
    private Color color;
    private CardType cardType ;
    private int value;

    int getValue(){
        return this.value;
    }
    Color getColor(){
        return this.color ;
    }
    CardType getCardType(){
        return this.cardType;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public void setCardType(CardType cardType) {
        this.cardType = cardType;
    }
}

