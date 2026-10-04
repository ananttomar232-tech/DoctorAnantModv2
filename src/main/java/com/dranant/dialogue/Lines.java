package com.dranant.dialogue;

import net.minecraft.util.RandomSource;

/** All the Hinglish dialogue used by patients, staff, the shopkeeper and the boss. */
public final class Lines {
    /** Mild problems: fixed with medicine at the counter (Line 1). */
    public static final String[] MILD_PROBLEMS = {
            "Mujhe 3 din se bukhaar hai!",
            "Sir mein bahut dard hai bhaiya...",
            "Khansi ruk hi nahi rahi! *khoo khoo*",
            "Creeper ne daraya, BP high ho gaya!",
            "Pet kharab hai, kal 64 golgappe khaye!",
            "Phantom ki wajah se neend nahi aati!",
            "Mining karte karte haath mein moch aa gayi.",
            "Zombie ne thoda sa kaat liya... bas thoda sa.",
            "Aankhon mein jalan hai, lava dekh liya tha.",
            "Gala kharab hai, gaana nahi ga pa raha!",
            "Bees ne kaat liya, honey churane gaya tha.",
            "Daant mein dard hai, bread bahut sakht thi!"
    };
    /** Severe problems: need the lab bed / stretcher (Line 2). */
    public static final String[] SEVERE_PROBLEMS = {
            "Skeleton ka teer lag gaya! Jaldi karo!",
            "Haddi toot gayi, chal nahi pa raha!",
            "Lava mein gir gaya tha... sab jal gaya!",
            "Wither ne attack kiya... saans nahi aa rahi!",
            "Bahut chakkar aa rahe hain doctor saab...",
            "Pillager ne maara, bahut khoon beh raha hai!",
            "Warden se mil ke aaya hoon... kuch sunai nahi deta!",
            "Pahad se gir gaya, poora body dard kar raha hai!"
    };
    public static final String[] WAITING = {
            "Kitni der lagegi bhai?",
            "Line aage badhao!",
            "Aah... dard ho raha hai...",
            "Mere aage wala bahut time le raha hai!",
            "Suna hai Dr. Anant ki dawai jaadu hai!",
            "Mujhe pehle jaane do, emergency hai!",
            "Yahan WiFi hai kya?",
            "Mummy ne bola tha Dr. Anant ke paas hi jaana."
    };
    public static final String[] HAGGLE = {
            "Bhaiya thoda discount do na!",
            "Itna mehenga? Gaon mein 1 diamond mein milta hai!",
            "Main roz aata hoon, regular customer hoon!",
            "Dr. Anant ne bola tha free milegi!",
            "Do le lunga, ek free do!",
            "Mere paas sirf emeralds hain, chalega?"
    };
    public static final String[] PHARMACIST_REPLY = {
            "Rate fixed hai bhai, Dr. Anant ki dawai hai!",
            "Chalo, aapke liye special price!",
            "Peeche line lambi hai, jaldi bolo!",
            "Emeralds nahi, sirf diamonds chalte hain!",
            "Isse sasta poore server mein nahi milega!",
            "Theek hai, thoda kam kar deta hoon."
    };
    public static final String[] PHARMACIST_ADVICE = {
            "Ye lo, din mein do baar khana ke baad!",
            "Ye dawai lo, 2 din mein theek ho jaoge!",
            "Thanda paani mat peena, theek?",
            "Aaram karo aur phantom se door raho!"
    };
    public static final String[] DEAL = {
            "Theek hai, ye lo diamonds!",
            "Chalo, le lo. Dhanyavaad!",
            "Okay okay, deal pakki!",
            "Dr. Anant zindabad! Ye lo payment."
    };
    public static final String[] CURED_MILD = {
            "Ab bilkul theek lag raha hai!",
            "Wah! Dawai ne turant kaam kiya!",
            "Main sabko bataunga is clinic ke baare mein!"
    };
    public static final String[] SCIENTIST_TREAT = {
            "Ghabrao mat, Dr. Anant ka formula hai!",
            "Bas ek chhota sa injection...",
            "Experimental treatment shuru!",
            "Lete raho, hilna mat!"
    };
    public static final String[] PATIENT_ON_BED = {
            "Aah! Injection se darr lagta hai!",
            "Kya ye safe hai?!",
            "Mujhe glowing kyun lag raha hai?",
            "Ouch! Dheere doctor saab!"
    };
    public static final String[] CURED_SEVERE = {
            "Wah! Main bilkul theek ho gaya!",
            "Dr. Anant zindabad! Ye lo 64 diamonds!",
            "Chamatkar! Main fir se chal sakta hoon!"
    };
    public static final String[] NO_PHARMACIST = {
            "Koi hai? Pharmacist kahan hai?",
            "Counter khaali hai! Hello?"
    };
    public static final String[] SELLER_FRIENDLY = {
            "Aao aao! Sabse sasti dawai yahin milegi!",
            "Clinic upgrade le lo, business double ho jayega!",
            "Aaj Immortality Elixir pe special offer!",
            "Arre bade log! Kya chahiye aapko?",
            "Level 6 Mega Hospital dekha? Ekdum jhakaas!",
            "Diamonds hain? To sab kuch hai!"
    };
    public static final String[] SELLER_RUDE = {
            "Kharidna hai to kharido, time waste mat karo!",
            "Diamonds hain bhi jeb mein?",
            "Haath mat lagao, pehle paise dikhao!",
            "Hmph. Ek aur window shopper...",
            "Mera dimaag mat khao, line mein aao!",
            "Ye dukaan hai, museum nahi!"
    };
    public static final String[] SELLER_THANKS = {
            "Shukriya! Fir aana!",
            "Badhiya choice, saab!",
            "Paisa vasool cheez hai ye!"
    };
    public static final String[] SELLER_BROKE = {
            "Paise kam hain! Pehle kamao, fir aao.",
            "Itne diamonds mein to sirf hawa milegi!"
    };
    public static final String[] BOSS = {
            "GRRAAAHHH!!",
            "EXPIRED... SERUM... RAAAGH!",
            "DOCTORRRR!!!",
            "*blood-curdling roar*"
    };

    public static String pick(String[] pool, RandomSource random) {
        return pool[random.nextInt(pool.length)];
    }

    private Lines() {}
}
