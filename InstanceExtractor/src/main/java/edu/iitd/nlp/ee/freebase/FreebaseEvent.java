package edu.iitd.nlp.ee.freebase;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import edu.iitd.nlp.ee.stringsearch.ACTrie;
import edu.iitd.nlp.ee.utils.CommonFunctions;
import edu.iitd.nlp.ee.utils.WordnetWrapper;

public class FreebaseEvent {

	public String name;
	
	public String mid;
	
	public String wikiId;
	
	public HashSet<String> aliases;
	
	public ArrayList<String> locations;
	
	public String startDateStr;
	
	public String endDateStr;
	
	public String recurrentEvent;
	
	public String recurrentEventMid;
	
	public Date startDate;
	
	public Date endDate;
	
	
	
	@Override
	public String toString() {
		return  aliases.toString();
	}

	public FreebaseEvent(String name) {
		this.name = name;
		aliases = new HashSet<String>();
		locations = new ArrayList<String>();
	} 

	public boolean isAlias(String str) {
		if(aliases.contains(str.toLowerCase().trim()))
			return true;
		return false;
	}
	
	public String getLocations() {
		
		StringBuilder sb = new StringBuilder();
		sb.append("[ ");
		for (int i = 0; i < locations.size(); i++) {
			if(i != 0)
				sb.append(", ");
			sb.append(locations.get(i));
		}
		sb.append(" ]");
		
		return sb.toString();
	}
	
	public String getStartDate() {
		return startDateStr;
	}
	
	public static enum Mode {
		indexer, train, test;
	}

	public static boolean IsAliasInvalid(String alias) {

		if (alias.equals("null")) // remove events with no names
			return true;
		if (alias.length() == 4 && alias.replaceAll("[0-9]+", "").equals("")) // remove all years
			return true;
		if (alias.length() == 5 && alias.replaceAll("[0-9]+", "").equals("s")) // remove all decades 1960, 1970s etc
			return true;
		if (alias.equalsIgnoreCase("20th century") || alias.equalsIgnoreCase("late 20th century") || alias.equalsIgnoreCase("21st century")) // remove all centuries
			return true;
		
		String mention = alias;
		
		if(mention.trim().equals("money in the bank")
				|| mention.trim().equals("world cup")
				|| mention.trim().equals("over the edge")
				|| mention.trim().equals("brazil")
				|| mention.trim().equals("final resolution")
				|| mention.trim().equals("off")
				|| mention.trim().equals("golden eye")
				|| mention.trim().equals("advancing learning")
				|| mention.trim().equals("uw")
				|| mention.trim().equals("noir city")
				|| mention.trim().equals("images")
				|| mention.trim().equals("message to man")
				|| mention.trim().equals("summerslam")
				|| mention.trim().equals("winston")
				|| mention.trim().equals("ea sports")
				|| mention.trim().equals("amp energy")
				|| mention.trim().equals("uaw-ford")
				|| mention.trim().equals("evoke")
				|| mention.trim().equals("outrageous")
				|| mention.trim().equals("cesar")
				|| mention.trim().equals("césar")
				|| mention.trim().equals("filmfest")
				|| mention.trim().equals("anniversary edition")
				|| mention.trim().equals("against all odds")
				|| mention.trim().equals("annecy")
				|| mention.trim().equals("back to basics")
				|| mention.trim().equals("maximum halloween")
				|| mention.trim().equals("images with words")
				|| mention.trim().equals("the mag")
				|| mention.trim().equals("lifelock.com")
				|| mention.trim().equals("tropicana")
				|| mention.trim().equals("geico")
				|| mention.trim().equals("usg sheetrock")
				|| mention.trim().equals("verzio")
				|| mention.trim().equals("tropicana 400 presented by meijer")
				|| mention.trim().equals("ffs")
				|| mention.trim().equals("colcoa")
				|| mention.trim().equals("ryo")
				|| mention.trim().equals("tums quikpak")
				|| mention.trim().equals("tums fast relief")
				|| mention.trim().equals("napa autocare")
				|| mention.trim().equals("old dominion")
				|| mention.trim().equals("3m performance 400 presented by bondo")
				|| mention.trim().equals("pepsi 400 presented by devilbiss")
				|| mention.trim().equals("carfax")
				|| mention.trim().equals("gfs marketplace")
				|| mention.trim().equals("3m performance")
				|| mention.trim().equals("pepsi 400 presented by farmer jack")
				|| mention.trim().equals("pepsi 400 presented by meijer")
				|| mention.trim().equals("pure michigan")
				|| mention.trim().equals("fime")
				|| mention.trim().equals("vs championships")
				|| mention.trim().equals("doc review")
				|| mention.trim().equals("morbid")
				|| mention.trim().equals("green man")
				|| mention.trim().equals("youki")
				|| mention.trim().equals("proms")
				|| mention.trim().equals("2annas")
				|| mention.trim().equals("times clipper")
				|| mention.trim().equals("world games")
				|| mention.trim().equals("east winds")
				|| mention.trim().equals("worldfest")
				|| mention.trim().equals("proms")
				|| mention.trim().equals("annual festival")
				|| mention.trim().equals("san fermin")
				|| mention.trim().equals("san fermín")
				|| mention.trim().equals("exide nascar select batteries")
				|| mention.trim().equals("chevrolet monte carlo")
				|| mention.trim().equals("chevy rock & roll")
				|| mention.trim().equals("federated auto parts")
				|| mention.trim().equals("chevy monte carlo")
				|| mention.trim().equals("judgement day")
				|| mention.trim().equals("judgment day")
				|| mention.trim().equals("afo")
				|| mention.trim().equals("ff")
				|| mention.trim().equals("ffg")
				|| mention.trim().equals("vis")
				|| mention.trim().equals("turning point")
				|| mention.trim().equals("owr")
				|| mention.trim().equals("open championship")
				|| mention.trim().equals("counterparts")
				|| mention.trim().equals("new year's revolution")
				|| mention.trim().equals("new years revolution")
				|| mention.trim().equals("flahertiana")
				|| mention.trim().equals("lbs")
				|| mention.trim().equals("cinema city")
				|| mention.trim().equals("scotch cup")
				|| mention.trim().equals("unforgiven")
				|| mention.trim().equals("cinefranco")
				|| mention.trim().equals("cinéfranco")
				|| mention.trim().equals("neighborhood excellence 400 presented by bank of america")
				|| mention.trim().equals("mbna racepointsz")
				|| mention.trim().equals("mbna platinum")
				|| mention.trim().equals("best buy 400 benefiting student clubs for autism speaks")
				|| mention.trim().equals("mbna 400 \"a salute to heroes\"")
				|| mention.trim().equals("autism speaks")
				|| mention.trim().equals("mbna armed forces family")
				|| mention.trim().equals("advance auto parts")
				|| mention.trim().equals("goody's cool orange")
				|| mention.trim().equals("goody's body pain")
				|| mention.trim().equals("directv")
				|| mention.trim().equals("olympic")
				|| mention.trim().equals("cinanima")
				|| mention.trim().equals("lacs")
				|| mention.trim().equals("fnf")
				|| mention.trim().equals("comedy festival")
				|| mention.trim().equals("edition liff")
				|| mention.trim().equals("prudential world cup,")
				|| mention.trim().equals("prudential cup,")
				|| mention.trim().equals("burning man")
				|| mention.trim().equals("armegeddon")
				|| mention.trim().equals("faf")
				|| mention.trim().equals("nfl")
				|| mention.trim().equals("bl")
				|| mention.trim().equals("for rainbow")
				|| mention.trim().equals("coca-cola")
				|| mention.trim().equals("fict")
				|| mention.trim().equals("no way out")
				|| mention.trim().equals("gmc")
				|| mention.trim().equals("orma")
				|| mention.trim().equals("gff")
				|| mention.trim().equals("annual gff")
				|| mention.trim().equals("pro bowl")
				|| mention.trim().equals("film festival")
				|| mention.trim().equals("the international")
				|| mention.trim().equals("thin line")
				|| mention.trim().equals("videodance")
				|| mention.trim().equals("carsdirect.com")
				|| mention.trim().equals("shelby")
				|| mention.trim().equals("uaw-dodge")
				|| mention.trim().equals("uaw-daimlerchrysler")
				|| mention.trim().equals("las vegas")
				|| mention.trim().equals("kobalt tools")
				|| mention.trim().equals("friday the 13th")
				|| mention.trim().equals("guyu korea")
				|| mention.trim().equals("guyu")
				|| mention.trim().equals("sms")
				|| mention.trim().equals("samsung/radio shack")
				|| mention.trim().equals("samsung")
				|| mention.trim().equals("samsung/radioshack")
				|| mention.trim().equals("primestar")
				|| mention.trim().equals("harrah's")
				|| mention.trim().equals("olympic opening")
				|| mention.trim().equals("opening ceremony")
				|| mention.trim().equals("opening ceremonies")
				|| mention.trim().equals("olympics open")
				|| mention.trim().equals("hhm")
				|| mention.trim().equals("pepsi southern")
				|| mention.trim().equals("mountain dew southern")
				|| mention.trim().equals("sony open tennis")
				|| mention.trim().equals("sony ericsson open")
				|| mention.trim().equals("lipton championships")
				|| mention.trim().equals("lipton international players championships")
				|| mention.trim().equals("destination x")
				|| mention.trim().equals("mix nyc")
				|| mention.trim().equals("film by the sea")
				|| mention.trim().equals("supergame")
				|| mention.trim().equals("stozhary")
				|| mention.trim().equals("cinemed")
				|| mention.trim().equals("tomorrowland")
				|| mention.trim().equals("chitrakatha")
				|| mention.trim().equals("arctic man")
				|| mention.trim().equals("cinemagic")
				|| mention.trim().equals("up")
				|| mention.trim().equals("in your house")
				|| mention.trim().equals("influencers")
				|| mention.trim().equals("the influencers")
				|| mention.trim().equals("leap day")
				|| mention.trim().equals("february 29")
				|| mention.trim().equals("fic")
				|| mention.trim().equals("fant")
				|| mention.trim().equals("bangalore circus fire")
				|| mention.trim().equals("circus fire")
				|| mention.trim().equals("water docs")
				|| mention.trim().equals("hump!")
				|| mention.trim().equals("dth!")
				|| mention.trim().equals("guth gafa")
				|| mention.trim().equals("documentary film festival")
				|| mention.trim().equals("grammy")
				|| mention.trim().equals("la film fest")
				|| mention.trim().equals("zurlini retrospective")
				|| mention.trim().equals("avanca")
				|| mention.trim().equals("t/f")
				|| mention.trim().equals("royal rumble")
				|| mention.trim().equals("bfm")
				|| mention.trim().equals("afm")
				|| mention.trim().equals("tdf")
				|| mention.trim().equals("acm awards")
				|| mention.trim().equals("republican primary")
				|| mention.trim().equals("champions cup")
				|| mention.trim().equals("uaw/gm quality")
				|| mention.trim().equals("bank of america")
				|| mention.trim().equals("uaw-gm quality")
				|| mention.trim().equals("saratov sufferings")
				|| mention.trim().equals("ecu")
				|| mention.trim().equals("écu")
				|| mention.trim().equals("to snob")
				|| mention.trim().equals("sta-green")
				|| mention.trim().equals("camping world rv sales")
				|| mention.trim().equals("cittadella del corto")
				|| mention.trim().equals("la cittadella del corto")
				|| mention.trim().equals("fmf")
				|| mention.trim().equals("crown royal presents the samuel deeds 400 at the brickyard powered by bigmachinerecords.com")
				|| mention.trim().equals("crown royal presents the curtiss shaver 400 at the brickyard")
				|| mention.trim().equals("allstate 400 at the brickyard")
				|| mention.trim().equals("cinéfest")
				|| mention.trim().equals("bl")
				|| mention.trim().equals("cinéfest")
				|| mention.trim().equals("cinefest")
				|| mention.trim().equals("cinéfest sudbury")
				|| mention.trim().equals("cinefest sudbury")
				|| mention.trim().equals("animax")
				|| mention.trim().equals("underdox")
				|| mention.trim().equals("ozu")
				|| mention.trim().equals("fifa")
				|| mention.trim().equals("fisdn")
				|| mention.trim().equals("isdn")
				|| mention.trim().equals("signes de nuit")
				|| mention.trim().equals("sft")
				|| mention.trim().equals("victory road")
				|| mention.trim().equals("tropfest")
				|| mention.trim().equals("tropfest finalists")
				|| mention.trim().equals("laef")
				|| mention.trim().equals("25 fps")
				|| mention.trim().equals("durango film")
				|| mention.trim().equals("annual durango film")
				|| mention.trim().equals("side by side")
				|| mention.trim().equals("ars independent")
				|| mention.trim().equals("boc challenge")
				|| mention.trim().equals("around alonebl")
				|| mention.trim().equals("qfest")
				|| mention.trim().equals("darling-babies")
				|| mention.trim().equals("cine pe")
				|| mention.trim().equals("afi docs")
				|| mention.trim().equals("gtc")
				|| mention.trim().equals("charleston pride")
				|| mention.trim().equals("barcelona kia")
				|| mention.trim().equals("dlx")
				|| mention.trim().equals("third directors lounge")
				|| mention.trim().equals("rtf")
				|| mention.trim().equals("outview")
				|| mention.trim().equals("copa claro")
				|| mention.trim().equals("noir in festival")
				|| mention.trim().equals("paris cinéma")
				|| mention.trim().equals("paris cinema")
				|| mention.trim().equals("fipa")
				|| mention.trim().equals("zinegoak")
				|| mention.trim().equals("docudays")
				|| mention.trim().equals("the big chill")
				|| mention.trim().equals("big chill")
				|| mention.trim().equals("giraf")
				|| mention.trim().equals("chosen festival")
				|| mention.trim().equals("ffi")
				|| mention.trim().equals("fresno reel pride")
				|| mention.trim().equals("acdelco")
				|| mention.trim().equals("new hampshire")
				|| mention.trim().equals("sylvania")
				|| mention.trim().equals("farm aid on cmt")
				|| mention.trim().equals("dura lube/kmart")
				|| mention.trim().equals("dura lube")
				|| mention.trim().equals("cmf")
				|| mention.trim().equals("planet in focus")
				|| mention.trim().equals("shock stock")
				|| mention.trim().equals("reanimania")
				|| mention.trim().equals("flaiano prizes")
				|| mention.trim().equals("reeling")
				|| mention.trim().equals("i-know")
				|| mention.trim().equals("annual creteil-paris")
				|| mention.trim().equals("afiff")
				|| mention.trim().equals("creteil-paris")
				|| mention.trim().equals("mecal")
				|| mention.trim().equals("afi")
				|| mention.trim().equals("first kino")
				|| mention.trim().equals("paka")
				|| mention.trim().equals("shorts at moonlight")
				|| mention.trim().equals("s2f2")
				|| mention.trim().equals("pluk de nacht")
				|| mention.trim().equals("doctor who prom,")
				|| mention.trim().equals("big kristaps")
				|| mention.trim().equals("r2r")
				|| mention.trim().equals("espoo cine")
				|| mention.trim().equals("espoo ciné")
				|| mention.trim().equals("baf")
				|| mention.trim().equals("westfest")
				|| mention.trim().equals("fameko")
				|| mention.trim().equals("hours remain")
				|| mention.trim().equals("minecraft minithon")
				|| mention.trim().equals("christmas mini-thon!!")
				|| mention.trim().equals("dueling banjos minithon")
				|| mention.trim().equals("72 hours remain")
				|| mention.trim().equals("porsche classic")
				|| mention.trim().equals("fifa world player")
				|| mention.trim().equals("mercedescup")
				|| mention.trim().equals("march madness")
				|| mention.trim().equals("ic docs")
				|| mention.trim().equals("flatpack")
				|| mention.trim().equals("frameline")
				|| mention.trim().equals("pink apple")
				|| mention.trim().equals("kino polska")
				|| mention.trim().equals("a championship")
				|| mention.trim().equals("dok.fest")
				|| mention.trim().equals("kinoki")
				|| mention.trim().equals("hkfff")
				|| mention.trim().equals("cinecon")
				|| mention.trim().equals("out on film")
				|| mention.trim().equals("docpoint")
				|| mention.trim().equals("africa alive")
				|| mention.trim().equals("docfest")
				|| mention.trim().equals("train robbery")
				|| mention.trim().equals("tricky women")
				|| mention.trim().equals("hollyshorts")
				|| mention.trim().equals("fest")
				|| mention.trim().equals("reggae celebration")
				|| mention.trim().equals("tribute to the legends")
				|| mention.trim().equals("holy fire")
				|| mention.trim().equals("docufest")
				|| mention.trim().equals("gr")
				|| mention.trim().equals("edoc")
				|| mention.trim().equals("court metrage")
				|| mention.trim().equals("shortest day")
				|| mention.trim().equals("le jour le plus court")
				|| mention.trim().equals("the shortest day")
				|| mention.trim().equals("gene kelly")
				|| mention.trim().equals("dead channels")
				|| mention.trim().equals("งานประกาศผลรางวัลเกมและเทคโนโลยี vgx")
				|| mention.trim().equals("vga")
				|| mention.trim().equals("งานประกาศผลรางวัล vga")
				|| mention.trim().equals("vgas")
				|| mention.trim().equals("vgx")
				|| mention.trim().equals("งานประกาศผลรางวัลวิดีโอเกม vgx")
				|| mention.trim().equals("bmw open")
				|| mention.trim().equals("urban tv")
				|| mention.trim().equals("jcc")
				|| mention.trim().equals("abp")
				|| mention.trim().equals("food city")
				|| mention.trim().equals("fifi")
				|| mention.trim().equals("mostra")
				|| mention.trim().equals("auto club")
				|| mention.trim().equals("california 500 presented by napa")
				|| mention.trim().equals("napa auto parts")
				|| mention.trim().equals("fqf")
				|| mention.trim().equals("cyber sunday")
				|| mention.trim().equals("kentucky oaks")
				|| mention.trim().equals("coke zero 400 powered by coca-cola")
				|| mention.trim().equals("coke zero")
				|| mention.trim().equals("nyc shorts")
				|| mention.trim().equals("gardner denver")
				|| mention.trim().equals("sfw")
				|| mention.trim().equals("woh")
				|| mention.trim().equals("woh- may")
				|| mention.trim().equals("woh- november")
				|| mention.trim().equals("dances with films")
				|| mention.trim().equals("utr")
				|| mention.trim().equals("fleet week")
				|| mention.trim().equals("ifq")
				|| mention.trim().equals("dura-lube/big kmart")
				|| mention.trim().equals("gm goodwrench service plus")
				|| mention.trim().equals("dura-lube/kmart")
				|| mention.trim().equals("offf")
				|| mention.trim().equals("hot docs")
				|| mention.trim().equals("real madrid c.f. vs. atlético madrid")
				|| mention.trim().equals("real madrid c.f. vs. atletico madrid")
				|| mention.trim().equals("eattle true independent film festival")
				|| mention.trim().equals("aaron's")
				|| mention.trim().equals("talladega")
				|| mention.trim().equals("goto")
				|| mention.trim().equals("jaoo")
				|| mention.trim().equals("aaa texas")
				|| mention.trim().equals("dickies")
				|| mention.trim().equals("camping world rv 400 presented by coleman")
				|| mention.trim().equals("protection one")
				|| mention.trim().equals("lifelock")
				|| mention.trim().equals("banquet 400 presented by conagra foods")
				|| mention.trim().equals("boat race")
				|| mention.trim().equals("the boat race")
				|| mention.trim().equals("daytona")
				|| mention.trim().equals("camerimage")
				|| mention.trim().equals("plus camerimage")
				|| mention.trim().equals("sliff")
				|| mention.trim().equals("cibra")
				|| mention.trim().equals("bass pro shops mbna")
				|| mention.trim().equals("bass pro shops")
				|| mention.trim().equals("pep boys auto")
				|| mention.trim().equals("advocare")
				|| mention.trim().equals("hard justice")
				|| mention.trim().equals("hardcore justice")
				|| mention.trim().equals("lenox industrial tools")
				|| mention.trim().equals("new england")
				|| mention.trim().equals("jiffy lube")
				|| mention.trim().equals("thatlook.com")
				|| mention.trim().equals("backlash -")
				|| mention.trim().equals("camping world rv")
				|| mention.trim().equals("mbna america")
				|| mention.trim().equals("mbna gold")
				|| mention.trim().equals("camping world rv 400 presented by aaa")
				|| mention.trim().equals("mbna nascar racepoints")
				|| mention.trim().equals("mbna cal ripken, jr.")
				|| mention.trim().equals("dodge dealers")
				|| mention.trim().equals("mbna.com")
				|| mention.trim().equals("mbna all-american heroes")
				|| mention.trim().equals("cinema truth stand")
				|| mention.trim().equals("cinema verite")
				|| mention.trim().equals("international tribunes")
				|| mention.trim().equals("sound unseen")
				|| mention.trim().equals("10 fest")
				|| mention.trim().equals("sitges")
				|| mention.trim().equals("afw")
				|| mention.trim().equals("bad blood")
				|| mention.trim().equals("ide")
				|| mention.trim().equals("quicken loans")
				|| mention.trim().equals("miller lite")
				|| mention.trim().equals("kmart")
				|| mention.trim().equals("kmart 400 presented by castrol super clean")
				|| mention.trim().equals("citizens bank")
				|| mention.trim().equals("dhl")
				|| mention.trim().equals("batman begins")
				|| mention.trim().equals("3m performance 400 presented by post-it picture paper")
				|| mention.trim().equals("sirius satellite radio")
				|| mention.trim().equals("bound for glory")
				|| mention.trim().equals("survivor series")
				|| mention.trim().equals("oil crisis")
				|| mention.trim().equals("energy crisis")
				|| mention.trim().equals("full moon")
				|| mention.trim().equals("over the limit")
				|| mention.trim().equals("edition motelx")
				|| mention.trim().equals("annual awards")
				|| mention.trim().equals("awards")
				|| mention.trim().equals("game at radio city")
				|| mention.trim().equals("if3")
				|| mention.trim().equals("film palace fest")
				|| mention.trim().equals("horrorfest")
				|| mention.trim().equals("8 films to die for")
				|| mention.trim().equals("horrorfest ii")
				|| mention.trim().equals("newfest")
				|| mention.trim().equals("new fest")
				|| mention.trim().equals("skena up")
				|| mention.trim().equals("fantastic'arts")
				|| mention.trim().equals("eight edition")
				|| mention.trim().equals("ninth edition")
				|| mention.trim().equals("sixth edition")
				|| mention.trim().equals("exis")
				|| mention.trim().equals("flic")
				|| mention.trim().equals("hcaf")
				|| mention.trim().equals("xx mistrzostwa swiata w piłce noznej finał")
				|| mention.trim().equals("xx mistrzostwa świata w piłce nożnej finał")
				|| mention.trim().equals("uefa euro")
				|| mention.trim().equals("uefa euro football")
				|| mention.trim().equals("uefa euro championship")
				|| mention.trim().equals("european nations' cup")
				|| mention.trim().equals("uefa")
				|| mention.trim().equals("dirty dutch")
				|| mention.trim().equals("ol:m")
				|| mention.trim().equals("fantaspoa")
				|| mention.trim().equals("dvf")
				|| mention.trim().equals("aqff2009")
				|| mention.trim().equals("42. lubuskie lato filmowe")
				|| mention.trim().equals("llf")
				|| mention.trim().equals("off-courts")
				|| mention.trim().equals("advocare 500 (phoenix)")
				|| mention.trim().equals("checker auto parts 500 presented by pennzoil")
				|| mention.trim().equals("checker auto parts/dura lube 500k")
				|| mention.trim().equals("checker auto parts 500 presented by havoline")
				|| mention.trim().equals("checker o'reilly auto parts")
				|| mention.trim().equals("checker auto parts/dura lube")
				|| mention.trim().equals("checker auto parts")
				|| mention.trim().equals("distrital")
				|| mention.trim().equals("filmex")
				|| mention.trim().equals("sharp aquos")
				|| mention.trim().equals("sony hd")
				|| mention.trim().equals("pop secret")
				|| mention.trim().equals("fsf")
				|| mention.trim().equals("super 15")
				|| mention.trim().equals("subway fresh fit")
				|| mention.trim().equals("subway fresh")
				|| mention.trim().equals("colgate-palmolive masters")
				|| mention.trim().equals("tennis masters cup")
				|| mention.trim().equals("masters grand prix")
				|| mention.trim().equals("volvo masters")
				|| mention.trim().equals("commercial union assurance masters")
				|| mention.trim().equals("nabisco masters")
				|| mention.trim().equals("cineport")
				|| mention.trim().equals("cracker barrel old country store")
				|| mention.trim().equals("cracker barrel")
				|| mention.trim().equals("golden corral")
				|| mention.trim().equals("america's cup")
				|| mention.trim().equals("gafe")
				|| mention.trim().equals("no mercy")
				|| mention.trim().equals("chicago summit")
				|| mention.trim().equals("dodge challenger")
				|| mention.trim().equals("carolina dodge dealers")
				|| mention.trim().equals("transouth financial")
				|| mention.trim().equals("dodge avenger")
				|| mention.trim().equals("southern 500")
				|| mention.trim().equals("mall.com")
				|| mention.trim().equals("dodge charger")
				|| mention.trim().equals("southern 500 presented by godaddy.com")
				|| mention.trim().equals("xiaoman")
				|| mention.trim().equals("afx")
				|| mention.trim().equals("open cinema")
				|| mention.trim().equals("cen")
				|| mention.trim().equals("dead by dawn")
				|| mention.trim().equals("elimination chamber")
				|| mention.trim().equals("pocono")
				|| mention.trim().equals("blue swords")
				|| mention.trim().equals("filmstock")
				|| mention.trim().equals("barcelona independent")
				|| mention.trim().equals("annual edition")
				|| mention.trim().equals("encounters")
				|| mention.trim().equals("close encounters documentary laboratory")
				|| mention.trim().equals("cinema one originals")
				|| mention.trim().equals("sfl -")
				|| mention.trim().equals("save mart/kragen 350k")
				|| mention.trim().equals("toyota/savemart")
				|| mention.trim().equals("toyota/save mart")
				|| mention.trim().equals("save mart/kragen")
				|| mention.trim().equals("dodge/save mart")
				|| mention.trim().equals("hell in a cell")
				|| mention.trim().equals("qdoc")
				|| mention.trim().equals("no surrender")
				|| mention.trim().equals("next")
				|| mention.trim().equals("fest")
				|| mention.trim().equals("vibgyor")
				|| mention.trim().equals("tribal gathering")
				|| mention.trim().equals("cine ceará")
				|| mention.trim().equals("cine ceara")
				|| mention.trim().equals("george eastman house 360 | 365 film festival")
				|| mention.trim().equals("| 365 george eastman house film festival")
				|| mention.trim().equals("360 | 365 film festival")
				|| mention.trim().equals("austin city limits")
				|| mention.trim().equals("hfa")
				|| mention.trim().equals("italian film festival")
				|| mention.trim().equals("ashes")
				|| mention.trim().equals("the ashes")
				|| mention.trim().equals("atp masters")
				|| mention.trim().equals("curta cinema")
				|| mention.trim().equals("kino pavasaris")
				|| mention.trim().equals("alff")
				|| mention.trim().equals("fifdh")
				|| mention.trim().equals("pjff")
				|| mention.trim().equals("ccff")
				|| mention.trim().equals("baff")
				|| mention.trim().equals("giff")
				|| mention.trim().equals("ssff")
				|| mention.trim().equals("ziff")
				|| mention.trim().equals("bnff")
				|| mention.trim().equals("dff")
				|| mention.trim().equals("scff")
				|| mention.trim().equals("wiff")
				|| mention.trim().equals("u.s. open")
				|| mention.trim().equals("hff")
				|| mention.trim().equals("diff")
				|| mention.trim().equals("lff")
				|| mention.trim().equals("oiff")
				|| mention.trim().equals("bbff")
				|| mention.trim().equals("mjff")
				|| mention.trim().equals("pff")
				|| mention.trim().equals("human rights film festival")
				|| mention.trim().equals("sfiff")
				|| mention.trim().equals("tff")
				|| mention.trim().equals("international animation film festival")
				|| mention.trim().equals("bumbershoot")
				|| mention.trim().equals("cjff")
				|| mention.trim().equals("eiff")
				|| mention.trim().equals("laiff")
				|| mention.trim().equals("piff")
				|| mention.trim().equals("tcff")
				|| mention.trim().equals("euff")
				|| mention.trim().equals("cff")
				|| mention.trim().equals("sjff")
				|| mention.trim().equals("trebon film festival")
				|| mention.trim().equals("sfff")
				|| mention.trim().equals("kff")
				|| mention.trim().equals("icff")
				|| mention.trim().equals("river's edge international film festival")
				|| mention.trim().equals("sff")
				|| mention.trim().equals("mbna racepoints")
				|| mention.trim().equals("wff")
				|| mention.trim().equals("nyciff")
				|| mention.trim().equals("men's world ice hockey championships")
				|| mention.trim().equals("tjff")
				|| mention.trim().equals("goody's headache powder")
				|| mention.trim().equals("viff")
				|| mention.trim().equals("ajff")
				|| mention.trim().equals("american film festival")
				|| mention.trim().equals("ciff")
				|| mention.trim().equals("asian film festival")
				|| mention.trim().equals("dbff")
				|| mention.trim().equals("iffi")
				|| mention.trim().equals("european film market")
				|| mention.trim().equals("bcs national championship game")
				|| mention.trim().equals("siff")
				|| mention.trim().equals("bff")
				|| mention.trim().equals("dciff")
				|| mention.trim().equals("fff")
				|| mention.trim().equals("italian film festival")
				|| mention.trim().equals("bhff")
				|| mention.trim().equals("jff")
				|| mention.trim().equals("kiff")
				|| mention.trim().equals("ifff")
				|| mention.trim().equals("international short film festival")
				|| mention.trim().equals("nff")
				|| mention.trim().equals("bombay international film festival")
				|| mention.trim().equals("hiff")
				|| mention.trim().equals("rff")
				|| mention.trim().equals("lsff")
				|| mention.trim().equals("vff")
				|| mention.trim().equals("wcff")
				|| mention.trim().equals("zff")
				|| mention.trim().equals("aisff")
				|| mention.trim().equals("aiff")
				|| mention.trim().equals("laff")
				|| mention.trim().equals("rnff")
				|| mention.trim().equals("aaff")
				|| mention.trim().equals("liff")
				|| mention.trim().equals("msff")
				|| mention.trim().equals("aff")
				|| mention.trim().equals("wjff")
				|| mention.trim().equals("international film festival")
				|| mention.trim().equals("eff")
				|| mention.trim().equals("european union film festival")
				|| mention.trim().equals("iff")
				|| mention.trim().equals("annual biff")
				|| mention.trim().equals("iffla")
				|| mention.trim().equals("mgff")
				|| mention.trim().equals("mff")
				|| mention.trim().equals("fiff")
				|| mention.trim().equals("hkiff")
				|| mention.trim().equals("olympic games")
				|| mention.trim().equals("uff")
				|| mention.trim().equals("fmff")
				|| mention.trim().equals("european film festival")
				|| mention.trim().equals("bsff")
				|| mention.trim().equals("one world")
				|| mention.trim().equals("in the city")
				|| mention.trim().equals("world war")
				|| mention.trim().equals("the metropolitan")
				|| mention.trim().equals("the combination")
				|| mention.trim().equals("infest")
				|| mention.trim().equals("saturdays")
				|| mention.trim().equals("the ball")
				|| mention.trim().equals("black & white")
				|| mention.trim().equals("the galaxy")
				|| mention.trim().equals("beacons")
				|| mention.trim().equals("visions")
				|| mention.trim().equals("the party")
				|| mention.trim().equals("all together")
				|| mention.trim().equals("the bmw")
				|| mention.trim().equals("the shorts")
				|| mention.trim().equals("the summit")
				|| mention.trim().equals("high end")
				|| mention.trim().equals("the football league")
				|| mention.trim().equals("break the ice")
				|| mention.trim().equals("rise up")
				|| mention.trim().equals("field day")
				|| mention.trim().equals("independence day")
				|| mention.trim().equals("infiltrate")
				|| mention.trim().equals("skirmishes")
				|| mention.trim().equals("bmw")
				|| mention.trim().equals("the dinner party")
				|| mention.trim().equals("dinner party")
				|| mention.trim().equals("elmo")
				|| mention.trim().equals("midlands")
				|| mention.trim().equals("big game")
				|| mention.trim().equals("pink ribbon")
				|| mention.trim().equals("national league")
				|| mention.trim().equals("the fest")
				|| mention.trim().equals("club day")
				|| mention.trim().equals("the gathering")
				|| mention.trim().equals("gathering")
				|| mention.trim().equals("southbound")
				|| mention.trim().equals("rock it")
				|| mention.trim().equals("first night")
				|| mention.trim().equals("center of gravity")
				|| mention.trim().equals("strata")
				|| mention.trim().equals("together as one")
				|| mention.trim().equals("complete works")
				|| mention.trim().equals("on the waterfront")
				|| mention.trim().equals("the great escape")
				|| mention.trim().equals("great escape")
				|| mention.trim().equals("constellations")
				|| mention.trim().equals("advertising age")
				|| mention.trim().equals("incubate")
				|| mention.trim().equals("fresh faces")
				|| mention.trim().equals("with full force")
				|| mention.trim().equals("soundwave")
				|| mention.trim().equals("garden party")
				|| mention.trim().equals("forbidden fruit")
				|| mention.trim().equals("porin")
				|| mention.trim().equals("ase")
				|| mention.trim().equals("mm2")
				|| mention.trim().equals("square roots")
				|| mention.trim().equals("car chase")
				|| mention.trim().equals("illuminations")
				|| mention.trim().equals("stage 5")
				|| mention.trim().equals("constellations")
				|| mention.trim().equals("match day")
				|| mention.trim().equals("beautiful days"))
			return true;
		
		return false;
	}
	
	/*
	 * Return contents of a file, line by line.
	 */
	public static ArrayList<String> readFileLineByLine(String file)
			throws IOException {

		BufferedReader reader = new BufferedReader(new FileReader(file));
		String line = null;
		ArrayList<String> contents = new ArrayList<String>();

		try {
			while ((line = reader.readLine()) != null)
				contents.add(line);

			return contents;
		} finally {
			reader.close();
		}
	}
	
	public static HashSet<String> FilterFreebaseEventsByTopic(String topicFile) throws IOException {
		
		ArrayList<String> contents = readFileLineByLine(topicFile);
		HashSet<String> eventMids = new HashSet<String>();
		
		for (int i = 0; i < contents.size(); i++) {

			String[] lineContents = contents.get(i).split("\\t");

			String eventName = lineContents[0].trim();
			String eventMid = lineContents[1].trim();

			if (IsAliasInvalid(eventName))
				continue;

			String[] types = lineContents[3].split("@#@");
			HashSet<String> typeSet = new HashSet<String>();
			for (int j = 0; j < types.length; j++)
				typeSet.add(types[j]);

			if (typeSet.contains("/spaceflight/space_mission")
					|| typeSet.contains("/computer/software"))
				continue;
				
			eventMids.add(eventMid);
		}
			
		return eventMids;
	}
	
	public static ArrayList<FreebaseEvent> GetEventsFromJson(String jsonFile, String topicFile, String annotationMode) throws IOException {
		
		HashMap<String, ArrayList<FreebaseEvent>> recurrentEventMap = new HashMap<String, ArrayList<FreebaseEvent>>();
		
		HashSet<String> selectedMIDs = FilterFreebaseEventsByTopic(topicFile);
		JSONObject json = CommonFunctions.readJsonFromFile(jsonFile);
		JSONArray resultList = (JSONArray) json.get("result");
		
		int totalEventInstances = 0;
		
		for (int i = 0; i < resultList.size(); i++) {

			try {

				JSONObject resultJson = (JSONObject) resultList.get(i);

				String mid = (String) resultJson.get("mid");
				
				if(!selectedMIDs.contains(mid))
					continue;
				
				String eventName = (String) resultJson.get("name");
				if(eventName == null)
					continue;
				
				if(resultJson.get("/time/event/instance_of_recurring_event") == null)
					continue;
				
				FreebaseEvent fbEvent = new FreebaseEvent(eventName);
				fbEvent.mid = mid;
				
				fbEvent.startDateStr = (String) resultJson.get("/time/event/start_date");
				String yearOfStartDateStr = fbEvent.startDateStr.substring(0, 4);
				try {
					int year = Integer.parseInt(yearOfStartDateStr);
					if (year < 1960)
						continue;
				} catch (Exception e) {
					continue;
				}
				
				fbEvent.endDateStr = (String) resultJson.get("/time/event/end_date");
				fbEvent.recurrentEvent = (String) ((JSONObject) resultJson.get("/time/event/instance_of_recurring_event")).get("name");
				
				
				JSONArray wikiIdArray = (JSONArray) resultJson
						.get("/wikipedia/topic/en_id");
				if (wikiIdArray != null && wikiIdArray.size() > 0) {
					fbEvent.wikiId = (String) wikiIdArray.get(0);
				}
				
				JSONArray aliasArray = (JSONArray) resultJson
						.get("/common/topic/alias");
				fbEvent.aliases.add(eventName.toLowerCase().trim());
				if (aliasArray != null) {
					for (int k = 0; k < aliasArray.size(); k++) {
						fbEvent.aliases.add(((String) aliasArray.get(k)).toLowerCase().trim());
					}
				}
				
				JSONArray locationArray = (JSONArray) resultJson
						.get("/time/event/locations");
				
				if (locationArray != null) {
					for (int k = 0; k < locationArray.size(); k++) {
						fbEvent.locations.add((String) ((JSONObject)locationArray.get(k)).get("name"));
					}
				}
				
				String key = fbEvent.recurrentEvent;
				if(recurrentEventMap.containsKey(key)) {
					ArrayList<FreebaseEvent> instances = recurrentEventMap.get(key);
					instances.add(fbEvent);
					recurrentEventMap.put(key, instances);
				} else {
					ArrayList<FreebaseEvent> instances = new ArrayList<FreebaseEvent>();
					instances.add(fbEvent);
					recurrentEventMap.put(key, instances);
				}
				
				totalEventInstances++;
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		int start = 0;
		int end = recurrentEventMap.size();
		
		if(annotationMode.equalsIgnoreCase("train")) {
			end = recurrentEventMap.size()/2;
		}else if(annotationMode.equalsIgnoreCase("test")) {
			start = recurrentEventMap.size()/2;
		}
		
		int i=0;
		int addedInstances = 0;
		ArrayList<FreebaseEvent> selectedEvents = new ArrayList<FreebaseEvent>();
		for(Entry<String, ArrayList<FreebaseEvent>> e : recurrentEventMap.entrySet()) {
			if(i>= start && i< end) {
				selectedEvents.addAll(e.getValue());
				addedInstances+= e.getValue().size();
			}
			i++;
		}
		
		System.out.println("Selected " + (end-start) + " meta events out of " + recurrentEventMap.size());
		System.out.println("Selected " + addedInstances + " out of " + totalEventInstances);
		return selectedEvents;
	}
	
	public static HashMap<String, ArrayList<FreebaseEvent>> GetRecurrentEventsFromJson(String jsonFile, String topicFile, String allMetaEventsCsvFile, boolean onlyMetaEventsWithoutInstances) throws IOException {
			
			HashMap<String, ArrayList<FreebaseEvent>> recurrentEventMap = GetRecurrentEventsFromJson(jsonFile, topicFile);
			HashSet<String> allMetaEvents = ReadAllMetaEvents(allMetaEventsCsvFile);
			
			HashMap<String,ArrayList<FreebaseEvent>> recurrentEventsWithoutInstancesMap = new HashMap<String,ArrayList<FreebaseEvent>>();
			
			for(String metaEvent : allMetaEvents) {
				if(!recurrentEventMap.containsKey(metaEvent))
					recurrentEventsWithoutInstancesMap.put(metaEvent, new ArrayList<FreebaseEvent>());
			}
			
			if(onlyMetaEventsWithoutInstances)
				return recurrentEventsWithoutInstancesMap;
			else {
				recurrentEventMap.putAll(recurrentEventsWithoutInstancesMap);
				return recurrentEventMap;
			}
	}
	
	public static HashSet<String> ReadAllMetaEvents(String allMetaEventsCsvFile) throws IOException {
		 
		 HashSet<String> allMetaEvents = new HashSet<String>();
		 
		 BufferedReader br = new BufferedReader(new FileReader(allMetaEventsCsvFile));
		 String line = br.readLine();

	     while (line != null) {
	        allMetaEvents.add(line.toLowerCase().replace("&amp;", "&").trim());
	        line = br.readLine();
	     }
	     br.close();
		    
		 return allMetaEvents;
	 }
	
	public static HashMap<String, ArrayList<FreebaseEvent>> GetRecurrentEventsFromJson(String jsonFile, String topicFile) throws IOException {
		
		int instancesCount = 0;
		int instancesWithSD = 0;
		HashMap<String, ArrayList<FreebaseEvent>> recurrentEventMap = new HashMap<String, ArrayList<FreebaseEvent>>();
		
		HashSet<String> selectedMIDs = FilterFreebaseEventsByTopic(topicFile);
		JSONObject json = CommonFunctions.readJsonFromFile(jsonFile);
		JSONArray resultList = (JSONArray) json.get("result");
		
		for (int i = 0; i < resultList.size(); i++) {
			
			JSONObject resultJson = (JSONObject) resultList.get(i);

			try {

				String mid = (String) resultJson.get("mid");
				
				if(!selectedMIDs.contains(mid))
					continue;
				
				String eventName = (String) resultJson.get("name");
				if(eventName == null)
					continue;
				
				if(resultJson.get("/time/event/instance_of_recurring_event") == null)
					continue;
				
				FreebaseEvent fbEvent = new FreebaseEvent(eventName);
				fbEvent.mid = mid;
				
				//if(eventName.toLowerCase().contains("yubari international"))
				//	System.out.println("");
				
				fbEvent.startDateStr = (String) resultJson.get("/time/event/start_date");
				String yearOfStartDateStr = fbEvent.startDateStr.substring(0, 4);
				int year = -1;
				try {
					year = Integer.parseInt(yearOfStartDateStr);
					if (year < 1960)
						continue;
				} catch (Exception e) {
					continue;
				}
				
				if(year > 2005) {
	 				instancesCount++;
					if(fbEvent.startDateStr.length() >= 10)
						instancesWithSD++;
				}
				
				fbEvent.endDateStr = (String) resultJson.get("/time/event/end_date");
				if(((JSONObject) resultJson.get("/time/event/instance_of_recurring_event")).get("name") == null)
					fbEvent.recurrentEvent = "";
				else {
					fbEvent.recurrentEvent = ((String) ((JSONObject) resultJson.get("/time/event/instance_of_recurring_event")).get("name")).replace("&amp;", "&");
					fbEvent.recurrentEventMid =((String) ((JSONObject) resultJson.get("/time/event/instance_of_recurring_event")).get("mid"));
				}
				
				JSONArray wikiIdArray = (JSONArray) resultJson
						.get("/wikipedia/topic/en_id");
				if (wikiIdArray != null && wikiIdArray.size() > 0) {
					fbEvent.wikiId = (String) wikiIdArray.get(0);
				}
				
				JSONArray aliasArray = (JSONArray) resultJson
						.get("/common/topic/alias");
				fbEvent.aliases.add(eventName.toLowerCase().replace("&amp;", "&").trim());
				if (aliasArray != null) {
					for (int k = 0; k < aliasArray.size(); k++) {
						fbEvent.aliases.add(((String) aliasArray.get(k)).toLowerCase().replace("&amp;", "&").trim());
					}
				}
				
				JSONArray locationArray = (JSONArray) resultJson
						.get("/time/event/locations");
				
				if (locationArray != null) {
					for (int k = 0; k < locationArray.size(); k++) {
						fbEvent.locations.add((String) ((JSONObject)locationArray.get(k)).get("name"));
					}
				}
				
				String key = fbEvent.recurrentEvent.toLowerCase().trim();
				
				if(recurrentEventMap.containsKey(key)) {
					ArrayList<FreebaseEvent> instances = recurrentEventMap.get(key);
					instances.add(fbEvent);
					recurrentEventMap.put(key, instances);
				} else {
					ArrayList<FreebaseEvent> instances = new ArrayList<FreebaseEvent>();
					instances.add(fbEvent);
					recurrentEventMap.put(key, instances);
				}
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		System.out.println(instancesCount + ":" + instancesWithSD);
		return recurrentEventMap;
	}

	public static ACTrie BuildACTrieForRecurrentEvent(HashMap<String, ArrayList<FreebaseEvent>> metaEventsToEvents,
			WordnetWrapper wordnetWrapper) throws IOException {


		ACTrie acTrie = new ACTrie();
		
		HashMap<String, String> fbEventAliasesToBeAdded = new HashMap<String, String>();
		HashMap<String, ArrayList<String>> duplicateAliases = new HashMap<String, ArrayList<String>>();
		
		for (Entry<String, ArrayList<FreebaseEvent>> e : metaEventsToEvents.entrySet()) {

			if(e.getKey().equalsIgnoreCase("generation"))
				continue;
			
			HashSet<String> metaNames = GetMetaEventNames(e.getKey(), e.getValue());
			
			for (String alias : metaNames) {
				
				String cleanedAlias = alias.replaceAll("[\\s]+", " ").toLowerCase().trim();

				if (IsAliasInvalid(cleanedAlias)) {
					continue;
				}

				// to remove commonly used English words like Bad (MJ Conecert Name) and Rose
				if (!cleanedAlias.contains(" ")) {
					if (wordnetWrapper.isWord(cleanedAlias) 
							&& !cleanedAlias.equals("oktoberfest")) {
						continue;
					}
				}
				
				if(duplicateAliases.containsKey(cleanedAlias)) {
					ArrayList<String> metaEvents = duplicateAliases.get(cleanedAlias);
					metaEvents.add(e.getKey());
					duplicateAliases.put(cleanedAlias, metaEvents);
					
				} else if(fbEventAliasesToBeAdded.containsKey(cleanedAlias)) {
					ArrayList<String> metaEvents = new ArrayList<String>();
					metaEvents.add(fbEventAliasesToBeAdded.get(cleanedAlias));
					metaEvents.add(e.getKey());
					duplicateAliases.put(cleanedAlias, metaEvents);
					
					fbEventAliasesToBeAdded.remove(cleanedAlias);
				} else {
					fbEventAliasesToBeAdded.put(cleanedAlias, e.getKey());
				}
			}
		}
		
		HashMap<String, String> newNames = new HashMap<String, String>();
		for(String duplicate : duplicateAliases.keySet()) {
			
			ArrayList<String> values = duplicateAliases.get(duplicate);
			String newName = values.get(0);
			
			fbEventAliasesToBeAdded.put(duplicate, newName);
			
			for(int i=1; i< values.size(); i++)
				newNames.put(values.get(i), newName);
		}
		
		HashSet<String> recurrentEvents = new HashSet<String>();
		for(Entry<String, String> e : fbEventAliasesToBeAdded.entrySet()) {
			String metaEventName = e.getValue();
			
			if(newNames.containsKey(e.getValue()))
				metaEventName = newNames.get(e.getValue());
			
			recurrentEvents.add(metaEventName);
			if(e.getKey().contains(","))
				acTrie.addMention(e.getKey().replace(",", " ").replaceAll("[ ]+", " "),  "META-EVENT", metaEventName, -1);
			
			acTrie.addMention(e.getKey(),  "META-EVENT", metaEventName, -1);
		}
		
		acTrie.InsertSuffixEdges();
		
		return acTrie;
	}
	
	public static ACTrie BuildACTrie(ArrayList<FreebaseEvent> fbEvents,
			WordnetWrapper wordnetWrapper) throws IOException {

		ACTrie acTrie = new ACTrie();
		
		HashMap<String, Integer> fbEventAliasesToBeAdded = new HashMap<String, Integer>();
		HashSet<String> dontAdd = new HashSet<String>();
		
		for (int i = 0; i < fbEvents.size(); i++) {

			String eventName = fbEvents.get(i).name;
			
			if (IsAliasInvalid(eventName))
				continue;

			for (String alias : fbEvents.get(i).aliases) {
				
				if(fbEvents.get(i).recurrentEvent != null && !fbEvents.get(i).recurrentEvent.trim().equals("")) {
					if(fbEvents.get(i).recurrentEvent.equalsIgnoreCase("generation"))
						continue;
					if(alias.equals(fbEvents.get(i).recurrentEvent.toLowerCase()))
						continue;
				}
				
				String cleanedAlias = alias.replaceAll("[\\s]+", " ")
						.trim();

				if (IsAliasInvalid(cleanedAlias) || dontAdd.contains(cleanedAlias))
					continue;

				// to remove commonly used English words like Bad (MJ Conecert Name) and Rose
				if (!cleanedAlias.contains(" ")) {
					if (wordnetWrapper.isWord(cleanedAlias))
						continue;
				}
				
				if(fbEventAliasesToBeAdded.containsKey(cleanedAlias)) {
					fbEventAliasesToBeAdded.remove(cleanedAlias);
					dontAdd.add(cleanedAlias);
				} else {
					fbEventAliasesToBeAdded.put(cleanedAlias, i);
				}
			}
		}
		
		for(Entry<String, Integer> e : fbEventAliasesToBeAdded.entrySet()) {
			FreebaseEvent fbe = fbEvents.get(e.getValue());
			acTrie.addMention(e.getKey(), "EVENT", fbe.mid, e.getValue());
		}
		
		acTrie.InsertSuffixEdges();
		
		return acTrie;
	}
	
	public static ACTrie BuildACTrie(HashMap<String, ArrayList<String>> metaEventToAliases,
			WordnetWrapper wordnetWrapper) throws IOException {

		ACTrie acTrie = new ACTrie();
		
		for(Entry<String, ArrayList<String>> e : metaEventToAliases.entrySet()) {

			for (String alias : e.getValue()) {
				
				String cleanedAlias = alias.replaceAll("[\\s]+", " ")
						.trim();

				if (IsAliasInvalid(cleanedAlias))
					continue;

				if (!cleanedAlias.contains(" ")) {
					if (wordnetWrapper.isWord(cleanedAlias))
						continue;
				}
				
				acTrie.addMention(cleanedAlias, "EVENT", "MID", 0);
			}
		}
		
		acTrie.InsertSuffixEdges();
		
		return acTrie;
	}
	
	public static boolean isCardinal(String word) {
		if(word.endsWith("st") && word.replace("st", "").replaceAll("[0-9]+", "").equals(""))
			return true;
		if(word.endsWith("nd") && word.replace("nd", "").replaceAll("[0-9]+", "").equals(""))
			return true;
		if(word.endsWith("rd") && word.replace("rd", "").replaceAll("[0-9]+", "").equals(""))
			return true;
		if(word.endsWith("th") && word.replace("th", "").replaceAll("[0-9]+", "").equals(""))
			return true;
		if(word.replaceAll("[0-9/]+", "").equals(""))
			return true;
		
		return false;
	}
	
	public static HashSet<String> GetMetaEventNames(String metaEventName, ArrayList<FreebaseEvent> events) {
		
		if(metaEventName.equals("Miss Louisiana USA")) {
			for(int i=0; i<events.size(); i++) {
				if(events.get(i).name.equals("Miss USA 1961")){
					events.remove(i);
					break;
				}	
			}
		}
		
		HashSet<String> metaNames = new HashSet<String>();
		if(metaEventName != null && !metaEventName.trim().equals("null") && !metaEventName.trim().equals("")) {
			metaEventName = metaEventName.toLowerCase().trim();
			metaNames.add(metaEventName);
			metaNames.add(StringUtils.stripAccents(metaEventName));
			
			if(metaEventName.startsWith("the")) {
				metaEventName = metaEventName.replaceFirst("the", "").trim();
				metaNames.add(metaEventName);
				metaNames.add(StringUtils.stripAccents(metaEventName));
			}
		}
		
		for(FreebaseEvent fe : events) {
		
			for(String alias : fe.aliases) {
				String[] words = alias.split("[ ]+");
				
				String metaName = "";
				boolean beginsWithDeterminer = false;
				for (int i = 0; i < words.length; i++) {
					if(i==0 && isCardinal(words[i]))
						continue;
					if(i==0 && words[i].equals("the")) {
						beginsWithDeterminer = true;
						continue;
					}
					if(i==1 && beginsWithDeterminer && isCardinal(words[i]))
						continue;
					if(i==words.length-1 && isCardinal(words[i]))
						continue;
					metaName += words[i] + " ";
				}
				
				metaName = metaName.trim();
				if(metaName.equals(""))
					continue;
				
				if(alias.equals(metaName))
					continue;
				
				metaNames.add(metaName);
				metaNames.add(StringUtils.stripAccents(metaName));
				
				if(metaName.startsWith("annual")) {
					metaName = metaName.replaceFirst("annual", "").trim();
					if(!metaName.equals("")) {
						metaNames.add(metaName);
						metaNames.add(StringUtils.stripAccents(metaName));
					}
				}

			}
		}
		
		return metaNames;
	}
}
