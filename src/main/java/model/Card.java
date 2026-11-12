package model;

public class Card {
    private Color color;
    private CardType cardType ;
    private int value;

    public Card(Color color , CardType cardType , int value ){
        this.color = color;
        this.cardType= cardType;
        this.value = value ;
    }

    public int getValue(){
        return this.value;
    }
    public Color getColor(){
        return this.color ;
    }
    public CardType getCardType(){
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

