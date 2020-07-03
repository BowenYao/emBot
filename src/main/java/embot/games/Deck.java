package embot.games;

import java.util.ArrayList;

public class Deck {
    private ArrayList<Card> cards;
    Deck(ArrayList<Card> cards){
        this.cards = cards;
    }
    Deck(boolean sorted){
        cards = new ArrayList<>(52);
        for(int y = 0; y < 4;y++) {
            for (int x = 0; x < 13; x++) {
                cards.add(x, new Card(y, x + 1));
            }
        } if(!sorted)
            shuffle();
    }
    public void shuffle(){
        ArrayList<Card> temp = new ArrayList<>();
        int size = cards.size();
        for(int x = 0; x < size; x++) {
            int num = (int) (Math.random() * cards.size());
            temp.add(cards.remove(num));
        }
        cards = temp;
    }
    public String toString(){
        String out = "";
        for(Card card:cards){
            out += card+ "\n";
        }
        return out;
    }

    Hand dealHand(int size){
        ArrayList<Card> hand = new ArrayList<>();
        System.out.println(size);
        for(int x = 0; x< size; x++){
            hand.add(cards.remove(0));
        }
        return new Hand(hand);
    }
    ArrayList<Card> getCards(){
        return cards;
    }
    Card drawCard(){
        return cards.remove(0);
    }
}
