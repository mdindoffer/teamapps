package org.teamapps.ux.i18n;

import java.util.*;

public class ResourceBundleTranslationProvider implements TranslationProvider {

	private List<Locale> languages;
	private List<String> keys;
	private Map<Locale, PropertyResourceBundle> resourceBundleByLocale = new HashMap<>();

	public ResourceBundleTranslationProvider(String baseName, Locale ... languages) {
		this(baseName, "properties", languages);
	}

	public ResourceBundleTranslationProvider(String baseName, String resourceFileSuffix, Locale ... languages) {
		this.languages = Arrays.asList(languages);
		Set<String> allKeys = new HashSet<>();
		for (Locale language : languages) {
			ResourceBundle bundle = ResourceBundle.getBundle(baseName, language, new UTF8Control(resourceFileSuffix));
			if (bundle instanceof PropertyResourceBundle) {
				PropertyResourceBundle propertyResourceBundle = (PropertyResourceBundle) bundle;
				resourceBundleByLocale.put(language, propertyResourceBundle);
				allKeys.addAll(propertyResourceBundle.keySet());
			}
		}
		keys = new ArrayList<>(allKeys);
	}

	@Override
	public List<Locale> getLanguages() {
		return languages;
	}

	@Override
	public List<String> getKeys() {
		return keys;
	}

	@Override
	public String getTranslation(String key, Locale locale) {
		Object value = resourceBundleByLocale.get(locale).handleGetObject(key);
		if (value != null) {
			return (String) value;
		} else {
			return null;
		}
	}
}
