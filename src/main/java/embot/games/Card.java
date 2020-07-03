package embot.games;

public class Card implements Comparable<Card>{
    private final int value;
    private static final String[] SUITS = {"Diamonds", "Clubs","Hearts","Spades"};
    private int suit;
    public Card(int suit, int value){
        this.suit = suit;
        this.value = value;
    }
    @Override
    public String toString(){
        String val;
        if(value == 13){
            val = "King";
        }else if (value == 12){
            val = "Queen";
        }else if (value == 11){
            val = "Jack";
        }else if (value == 1){
            val = "Ace";
        }else
            val = "" + value;
        return val + " of " + SUITS[suit];
    }
    public int compareTo(Card card){
        if(this.value>card.getValue()){
            return 1;
        }else if(this.value == card.getValue()){
            if(this.suit>card.getSuitValue())
                return 1;
            else if(this.suit==card.getSuitValue())
                return 0;
        }
        return -1;
    }
    public int compareToWithoutSuits(Card card){
        if(this.value>card.getValue()){
            return 1;
        }else if(this.value == card.getValue()){
            return 0;
        }
        return -1;
    }

    public int getValue(){
        return value;
    }
    public int getSuitValue(){
        return suit;
    }
    public String getSuit(){
        return SUITS[suit];
    }
}
