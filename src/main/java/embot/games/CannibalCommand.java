package embot.games;

import discord4j.core.event.domain.message.MessageCreateEvent;
import embot.core.*;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.Queue;
import java.util.Stack;

public class CannibalCommand extends OptionsCommand {
    private static String commandString = "cannibal";
    private static Stack<Character> lettersA, lettersB;
    private static final int[] distribution = new int[]{14, 6, 6, 8, 20, 6, 8, 10, 14, 4, 4, 8, 8, 12, 14, 6, 4, 12, 12, 14, 10, 4, 8, 2, 4, 2};
    private static final char[] alphabet = new char[]{'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'};

    static {
        reset(110);
    }

    public CannibalCommand() {
        super(commandString);
    }

    public CannibalCommand(MessageCreateEvent event) {
        super(commandString, event);
    }

    public static void reset(int numLetters) {
        lettersA = new Stack<>();
        lettersB = new Stack<>();
        for (int x = 0; x < numLetters; x++) {
            lettersA.add(generateLetter());
        }
    }

    public static char getLetter() {
        char letter = lettersA.pop();
        lettersB.add(letter);
        if (lettersA.isEmpty()) {
            Stack<Character> temp = lettersA;
            lettersA = lettersB;
            lettersB = temp;
            Collections.shuffle(lettersA);
        }
        return letter;
    }

    public static char generateLetter() {
        double random = Math.random() * 110;
        if (random < 7) {
            return 'A';
        } else if (random < 10) {
            return 'B';
        } else if (random < 13) {
            return 'C';
        } else if (random < 17) {
            return 'D';
        } else if (random < 27) {
            return 'E';
        } else if (random < 30) {
            return 'F';
        } else if (random < 34) {
            return 'G';
        } else if (random < 39) {
            return 'H';
        } else if (random < 46) {
            return 'I';
        } else if (random < 48) {
            return 'J';
        } else if (random < 50) {
            return 'K';
        } else if (random < 54) {
            return 'L';
        } else if (random < 58) {
            return 'M';
        } else if (random < 64) {
            return 'N';
        } else if (random < 71) {
            return 'O';
        } else if (random < 74) {
            return 'P';
        } else if (random < 76) {
            return 'Q';
        } else if (random < 82) {
            return 'R';
        } else if (random < 88) {
            return 'S';
        } else if (random < 95) {
            return 'T';
        } else if (random < 100) {
            return 'U';
        } else if (random < 102) {
            return 'V';
        } else if (random < 106) {
            return 'W';
        } else if (random < 107) {
            return 'X';
        } else if (random < 109) {
            return 'Y';
        } else {
            return 'Z';
        }
    }

    private static void resetStandard() {
        lettersA = new Stack<>();
        lettersB = new Stack<>();
        for (int x = 0; x < alphabet.length; x++) {
            for(int i = 0; i < distribution[x];i++){
                lettersA.add(alphabet[x]);
            }
        }
        Collections.shuffle(lettersA);
    }

    @Override
    public Mono<Void> run() {
        return getOptions()
                .flatMap(options -> {
                    if (options.length < 1)
                        return getEvent().getMessage().getChannel().flatMap(channel -> channel.createMessage("New letter: " + getLetter())).then();
                    else if (options[0].toLowerCase().equals("reset")) {
                        int numLetters = 100;
                        if (options.length > 1) {
                            try {
                                numLetters = Integer.parseInt(options[1]);
                                if(numLetters <1 || numLetters>10000){
                                    return malformedOptionsError(getEvent(),"Cannibal reset pool must have at least one and no more than ten thousand letters in it");
                                }
                            } catch (NumberFormatException e) {
                                if(options[1].equals("standard")){
                                    resetStandard();
                                    return getEvent().getMessage().getChannel().flatMap(channel->channel.createMessage("Cannibal pool reset to standard letter pool"));
                                }
                                return malformedOptionsError(getEvent(), "Cannibal reset must be followed by an integer or nothing at all");
                            }
                        }

                        reset(numLetters);
                        System.out.println("hi");
                        return getEvent().getMessage().getChannel().flatMap(channel -> channel.createMessage("Cannibal pool successfully reset"));
                    } else {
                        return malformedOptionsError(getEvent(), "Cannibal command malformed.");
                    }
                }).then();
    }
}
