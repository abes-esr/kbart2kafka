package fr.abes.kbart2kafka.utils;

import fr.abes.kbart2kafka.exception.IllegalDateException;
import fr.abes.kbart2kafka.exception.IllegalPackageException;
import fr.abes.kbart2kafka.exception.IllegalProviderException;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystems;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {

    public static String extractDomainFromUrl(String url) throws URISyntaxException {
        URI uri = new URI(url);
        String host = uri.getHost();
        if (host == null) {
            throw new URISyntaxException(url, "Format d'URL incorrect");
        }
        return host;
    }

    public static String extractProvider(String filename) throws IllegalProviderException {
        try {
            return filename.substring(0, filename.indexOf('_'));
        } catch (Exception e) {
            throw new IllegalProviderException(e);
        }
    }

    public static String extractPackageName(String filename) throws IllegalPackageException {
        try {
            if (filename.contains("_FORCE")) {
                String tempsStr = filename.substring(0, filename.indexOf("_FORCE"));
                return tempsStr.substring(tempsStr.indexOf('_') + 1, tempsStr.lastIndexOf('_'));
            } else if (filename.contains("_BYPASS")) {
                String tempStr = filename.substring(0, filename.indexOf("_BYPASS"));
                return tempStr.substring(tempStr.indexOf('_') + 1, tempStr.lastIndexOf('_'));
            } else {
                return filename.substring(filename.indexOf('_') + 1, filename.lastIndexOf('_'));
            }
        } catch (Exception e) {
            throw new IllegalPackageException(e);
        }
    }

    public static Date extractDateFilename(String filename) throws IllegalDateException {
        Date date = new Date();
        try {
            Matcher matcher = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})", Pattern.CASE_INSENSITIVE).matcher(filename);
            if (matcher.find()) {
                date = new SimpleDateFormat("yyyy-MM-dd").parse(matcher.group(1));
            }
            return date;
        } catch (Exception e) {
            throw new IllegalDateException(e);
        }
    }

    public static String reformatDateKbart(String dateToFormat) throws IllegalDateException {
        if (dateToFormat == null || dateToFormat.isEmpty())
            return "";
        Matcher matcher = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})", Pattern.CASE_INSENSITIVE).matcher(dateToFormat);
        if (matcher.find()) {
            return dateToFormat;
        }
        matcher = Pattern.compile("(\\d{4}-\\d{2})", Pattern.CASE_INSENSITIVE).matcher(dateToFormat);
        if (matcher.find()) {
            return dateToFormat + "-01";
        }
        matcher = Pattern.compile("(\\d{4})", Pattern.CASE_INSENSITIVE).matcher(dateToFormat);
        if (matcher.find()) {
            return dateToFormat + "-01-01";
        }
        throw new IllegalDateException("Format de date non reconnu, la date doit être au format YYYY ou YYYY-MM ou YYYY-MM-DD");
    }

    private static String checkDate(String dateToCheck) throws IllegalDateException {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            sdf.setLenient(false);
            Date parsedDate = sdf.parse(dateToCheck);

            // Obtenir la date actuelle (sans l'heure)
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date today = cal.getTime();
            // Vérifier si la date est dans le futur
            if (parsedDate.after(today)) {
                throw new IllegalDateException("La date fournie est invalide : " + dateToCheck);
            }
            return dateToCheck;
        } catch (ParseException e) {
            throw new IllegalDateException("La date fournie est invalide : " + dateToCheck);
        }
    }

    public static String extractFilename(String path) {
        if (path.contains(FileSystems.getDefault().getSeparator()))
            return path.substring(path.lastIndexOf(FileSystems.getDefault().getSeparator()) + 1);
        return path;
    }

    public static Boolean isDateBeforeOtherDate(String date, String otherDate) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        try {
            Date date1 = sdf.parse(date);
            Date date2 = sdf.parse(otherDate);
            return date1.before(date2);
        } catch (ParseException e) {
            // Handle parsing error (e.g., invalid date format)
            return false; // Or throw an exception, depending on your needs.
        }
    }

}
