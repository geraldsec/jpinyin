package com.github.stuxuhai.jpinyin;

import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * 汉字转拼音类
 *
 * @author stuxuhai (dczxxuhai@gmail.com)
 */
public final class PinyinHelper {
    private static List<String> dict = new ArrayList<String>();
    private static final Map<String, String> PINYIN_TABLE = PinyinResource.getPinyinResource();
    private static final Map<String, String> MUTIL_PINYIN_TABLE = PinyinResource.getMutilPinyinResource();
    private static final DoubleArrayTrie DOUBLE_ARRAY_TRIE = new DoubleArrayTrie();
    private static final String PINYIN_SEPARATOR = ","; // 拼音分隔符
    private static final char CHINESE_LING = '〇';
    private static final String ALL_UNMARKED_VOWEL = "aeiouv";
    private static final String ALL_MARKED_VOWEL = "āáǎàēéěèīíǐìōóǒòūúǔùǖǘǚǜ"; // 所有带声调的拼音字母

    static {
        for (String word : MUTIL_PINYIN_TABLE.keySet()) {
            dict.add(word);
        }
        Collections.sort(dict);
        DOUBLE_ARRAY_TRIE.build(dict);
    }

    private PinyinHelper() {
    }

    /**
     * 将带声调格式的拼音转换为数字代表声调格式的拼音
     * 
     * @param pinyinArrayString
     *            带声调格式的拼音
     * @return 数字代表声调格式的拼音
     */
    private static String[] convertWithToneNumber(String pinyinArrayString) {
        String[] pinyinArray = pinyinArrayString.split(PINYIN_SEPARATOR);
        for (int i = pinyinArray.length - 1; i >= 0; i--) {
            boolean hasMarkedChar = false;
            String originalPinyin = pinyinArray[i].replace("ü", "v"); // 将拼音中的ü替换为v

            for (int j = originalPinyin.length() - 1; j >= 0; j--) {
                char originalChar = originalPinyin.charAt(j);

                // 搜索带声调的拼音字母，如果存在则替换为对应不带声调的英文字母
                if (originalChar < 'a' || originalChar > 'z') {
                    int indexInAllMarked = ALL_MARKED_VOWEL.indexOf(originalChar);
                    int toneNumber = indexInAllMarked % 4 + 1; // 声调数
                    char replaceChar = ALL_UNMARKED_VOWEL.charAt((indexInAllMarked - indexInAllMarked % 4) / 4);
                    pinyinArray[i] = originalPinyin.replace(String.valueOf(originalChar), String.valueOf(replaceChar)) + toneNumber;
                    hasMarkedChar = true;
                    break;
                }
            }
            if (!hasMarkedChar) {
                // 找不到带声调的拼音字母说明是轻声，用数字5表示
                pinyinArray[i] = originalPinyin + "5";
            }
        }

        return pinyinArray;
    }

    /**
     * 将带声调格式的拼音转换为不带声调格式的拼音
     * 
     * @param pinyinArrayString
     *            带声调格式的拼音
     * @return 不带声调的拼音
     */
    private static String[] convertWithoutTone(String pinyinArrayString) {
        String[] pinyinArray;
        for (int i = ALL_MARKED_VOWEL.length() - 1; i >= 0; i--) {
            char originalChar = ALL_MARKED_VOWEL.charAt(i);
            char replaceChar = ALL_UNMARKED_VOWEL.charAt((i - i % 4) / 4);
            pinyinArrayString = pinyinArrayString.replace(String.valueOf(originalChar), String.valueOf(replaceChar));
        }
        // 将拼音中的ü替换为v
        pinyinArray = pinyinArrayString.replace("ü", "v").split(PINYIN_SEPARATOR);

        // 去掉声调后的拼音可能存在重复，做去重处理
        LinkedHashSet<String> pinyinSet = new LinkedHashSet<String>();
        for (String pinyin : pinyinArray) {
            pinyinSet.add(pinyin);
        }

        return pinyinSet.toArray(new String[pinyinSet.size()]);
    }

    /**
     * 将带声调的拼音格式化为相应格式的拼音
     * 
     * @param pinyinString
     *            带声调的拼音
     * @param pinyinFormat
     *            拼音格式：WITH_TONE_NUMBER--数字代表声调，WITHOUT_TONE--不带声调，WITH_TONE_MARK--带声调
     * @return 格式转换后的拼音
     */
    private static String[] formatPinyin(String pinyinString, PinyinFormat pinyinFormat) {
        if (pinyinFormat == PinyinFormat.WITH_TONE_MARK) {
            return pinyinString.split(PINYIN_SEPARATOR);
        } else if (pinyinFormat == PinyinFormat.WITH_TONE_NUMBER) {
            return convertWithToneNumber(pinyinString);
        } else if (pinyinFormat == PinyinFormat.WITHOUT_TONE) {
            return convertWithoutTone(pinyinString);
        }
        return new String[0];
    }

    /**
     * 将单个汉字转换为相应格式的拼音
     * 
     * @param c
     *            需要转换成拼音的汉字
     * @param pinyinFormat
     *            拼音格式：WITH_TONE_NUMBER--数字代表声调，WITHOUT_TONE--不带声调，WITH_TONE_MARK--带声调
     * @return 汉字的拼音
     */
    public static String[] convertToPinyinArray(char c, PinyinFormat pinyinFormat) {
        String pinyin = PINYIN_TABLE.get(String.valueOf(c));
        if ((pinyin != null) && (!"null".equals(pinyin))) {
            return formatPinyin(pinyin, pinyinFormat);
        }
        return new String[0];
    }

    /**
     * 将单个汉字转换成带声调格式的拼音
     * 
     * @param c
     *            需要转换成拼音的汉字
     * @return 字符串的拼音
     */
    public static String[] convertToPinyinArray(char c) {
        return convertToPinyinArray(c, PinyinFormat.WITH_TONE_MARK);
    }

    /**
     * 将字符串转换成相应格式的拼音
     * 
     * @param str
     *            需要转换的字符串
     * @param separator
     *            拼音分隔符
     * @param pinyinFormat
     *            拼音格式：WITH_TONE_NUMBER--数字代表声调，WITHOUT_TONE--不带声调，WITH_TONE_MARK--带声调
     * @return 字符串的拼音
     */
    public static String convertToPinyinString(String str, String separator, PinyinFormat pinyinFormat) {
        str = ChineseHelper.convertToSimplifiedChinese(str);
        StringBuilder sb = new StringBuilder();
        int i = 0;
        int strLen = str.length();
        while (i < strLen) {
            String substr = str.substring(i);
            List<Integer> commonPrefixList = DOUBLE_ARRAY_TRIE.commonPrefixSearch(substr);
            if (commonPrefixList.size() == 0) {
                char c = str.charAt(i);
                // 判断是否为汉字或者〇
                if (ChineseHelper.isChinese(c) || c == CHINESE_LING) {
                    String[] pinyinArray = convertToPinyinArray(str.charAt(i), pinyinFormat);
                    if (pinyinArray != null) {
                        for(int j=0; j<pinyinArray.length; j++)
                        {
                            sb.append(pinyinArray[j]);
                            if(pinyinArray.length>1 && j<pinyinArray.length-1)
                            {
                                sb.append("/");
                            }
                        }
                    } else {
                        sb.append(str.charAt(i));
                    }
                } else {
                    sb.append(c);
                }
                i++;
            } else {
                String words = dict.get(commonPrefixList.get(commonPrefixList.size() - 1));
                String[] pinyinArray = formatPinyin(MUTIL_PINYIN_TABLE.get(words), pinyinFormat);
                for (int j = 0, l = pinyinArray.length; j < l; j++) {
                    sb.append(pinyinArray[j]);
                    if (j < l - 1) {
                        sb.append(separator);
                    }
                }
                i += words.length();
            }

            if (i < strLen) {
                sb.append(separator);
            }
        }
        return sb.toString();
    }

    /**
     * 将字符串转换成带声调格式的拼音
     * 
     * @param str
     *            需要转换的字符串
     * @param separator
     *            拼音分隔符
     * @return 转换后带声调的拼音
     */
    public static String convertToPinyinString(String str, String separator) {
        return convertToPinyinString(str, separator, PinyinFormat.WITH_TONE_MARK);
    }

    /**
     * 判断一个汉字是否为多音字
     * 
     * @param c
     *            汉字
     * @return 判断结果，是汉字返回true，否则返回false
     */
    public static boolean hasMultiPinyin(char c) {
        String[] pinyinArray = convertToPinyinArray(c);
        if (pinyinArray != null && pinyinArray.length > 1) {
            return true;
        }
        return false;
    }

    /**
     * 获取字符串对应拼音的首字母
     * 
     * @param str
     *            需要转换的字符串
     * @return 对应拼音的首字母
     */
    public static String getShortPinyin(String str) {
        String separator = "#"; // 使用#作为拼音分隔符
        StringBuilder sb = new StringBuilder();

        //char[] charArray = new char[str.length()+100];
        StringBuilder sbResult  = new StringBuilder();
        for (int i = 0, len = str.length(); i < len; i++) {
            char c = str.charAt(i);

            // 首先判断是否为汉字或者〇，不是的话直接将该字符返回
            if (!ChineseHelper.isChinese(c) && c != CHINESE_LING) 
            {
                //charArray[i] = c;
                sbResult.append(c);
                sbResult.append(",");
            }
            else 
            {
                int j = i + 1;
                sb.append(c);

                // 搜索连续的汉字字符串
                while (j < len && (ChineseHelper.isChinese(str.charAt(j)) || str.charAt(j) == CHINESE_LING)) 
                {
                    sb.append(str.charAt(j));
                    j++;                    
                }
                String hanziPinyin = convertToPinyinString(sb.toString(), separator, PinyinFormat.WITHOUT_TONE);
                String[] pinyinArray = hanziPinyin.split(separator);
                for (int l=0; l<pinyinArray.length; l++) 
                {
                    String string = pinyinArray[l];
                    if(string.contains("/"))
                    {
                        String[] multiPinyinArray = string.split("/");
                        
                        for(int k=0;k<multiPinyinArray.length; k++)
                        {
                            String string1 = multiPinyinArray[k];
                            //charArray[i] = string1.charAt(0);
                             sbResult.append(string1.charAt(0));
                            //i++;                            
                            if(k<multiPinyinArray.length-1)
                            {
                               //charArray[i] = '/';
                               //i++;                                
                                sbResult.append("/");
                            }                           
                        }                                
                    }
                    else
                    {
                       //charArray[i] = string.charAt(0);                                
                       //i++;                    
                        sbResult.append( string.charAt(0));
                    }
                    
                    //if(l<pinyinArray.length-1)
                    //{
                       //charArray[i] = ',';                       
                       //i++;                                            
                        sbResult.append(",");
                        
                    //}
                    i++;
                }
                i--;
                sb.setLength(0);
            }
        }
       // return String.valueOf(charArray);
       return sbResult.toString();
    }

    public static void addPinyinDict(String path) {
        try {
            PINYIN_TABLE.putAll(PinyinResource.getResource(PinyinResource.newFileReader(path)));
        } catch (UnsupportedEncodingException e) {
            throw new PinyinException(e);
        } catch (FileNotFoundException e) {
            throw new PinyinException(e);
        }
    }

    public static void addMutilPinyinDict(String path) {
        try {
            MUTIL_PINYIN_TABLE.putAll(PinyinResource.getResource(PinyinResource.newFileReader(path)));
            dict.clear();
            DOUBLE_ARRAY_TRIE.clear();
            for (String word : MUTIL_PINYIN_TABLE.keySet()) {
                dict.add(word);
            }
            Collections.sort(dict);
            DOUBLE_ARRAY_TRIE.build(dict);
        } catch (UnsupportedEncodingException e) {
            throw new PinyinException(e);
        } catch (FileNotFoundException e) {
            throw new PinyinException(e);
        }
    }
    
	public static void main(String[] args) {
	    String str = "中国银行xgy大大5";
	//	String str = "中国行乘乐了么因日";
	    //PinyinHelper.convertToPinyinString(str, ",", PinyinFormat.WITH_TONE_MARK); // nǐ,hǎo,shì,jiè
	    //PinyinHelper.convertToPinyinString(str, ",", PinyinFormat.WITH_TONE_NUMBER); // ni3,hao3,shi4,jie4
	    
	    //获取多音字汉字首字线
            String strJcs = getMultiShortPinyin(str);
            System.out.println(strJcs);
	    
	    //获取多音字全拼
	   // String strQuanpins = getMultiQuanpin(str);
       // System.out.println(strQuanpins);
	}

	public static String getMultiQuanpin(String str) {
	    String quanpin = PinyinHelper.convertToPinyinString(str, ",", PinyinFormat.WITHOUT_TONE).toUpperCase();
	    //String quanpin = PinyinHelper.convertToPinyinString(str, "", PinyinFormat.WITH_TONE_MARK).toUpperCase();
	   // System.out.println(quanpin);
	    
	    List<String> qpList = getStringList(quanpin);
            StringBuffer sb2 = new StringBuffer();
            for(int i = 0; i<qpList.size(); i++)
            {
                String s1 = qpList.get(i);
                sb2.append(s1); 
                if(i<qpList.size()-1)
                {
                    sb2.append(",");
                }
            }    	    
            String strResult = sb2.toString();
	    
            return strResult;
            
	}

	public  static String getMultiShortPinyin(String str) {
		//System.out.println(str);
		
                //获取拼音首字母
                String pyjc = PinyinHelper.getShortPinyin(str).toUpperCase(); // nhsj
               // System.out.println(pyjc);
		
		List<String> jcList = getStringList(pyjc);
		
		StringBuffer sb = new StringBuffer();
		int nLength = pyjc.split(",").length;
		for(int i = 0; i<jcList.size(); i++)
		{
		    String s1 = jcList.get(i);
		    s1 = s1.substring(0,nLength);
		    //s1 = s1.replace("\s*", "");
		    sb.append(s1); 
		    if(i<jcList.size()-1)
		    {
		        sb.append(",");
		    }
		}
		String pyjcResult = sb.toString();
		//System.out.println(pyjcResult);
                return pyjcResult;
	}

	private static List<String> getStringList(String pyjc) {
		List<String> jcList = new ArrayList<String>();            
		String strResult = "";
		jcList.add(strResult);
		String strs[] = pyjc.split(",");
		for(String str1: strs)
		{
		    if(!str1.contains("/"))
		    {
		        for(int i=0; i<jcList.size(); i++)
		        {
		            strResult = jcList.get(i);
		            strResult = strResult + str1;
		            jcList.set(i,strResult);
		        }
		    }
		    else
		    {
		        String strs1[] = str1.split("/");
		        List<String> jcListTemp = new ArrayList<String>();  
		        jcListTemp.addAll(jcList);

		        for(int j=0; j<strs1.length; j++)
		        {
		            String str11 = strs1[j];
		            if(j==0)
		            {
		                for(int i=0; i<jcList.size(); i++)
		                {
		                    strResult = jcList.get(i);
		                    strResult = strResult + str11;
		                    jcList.set(i,strResult);
		                }
		            }
		            else
		            {
		                for(int i=0; i<jcListTemp.size(); i++)
		                {
		                    strResult = jcListTemp.get(i);
		                    strResult = strResult + str11;
		                    if(!jcList.contains(strResult))
		                    {    
		                        jcList.add(strResult);
		                    }
		                }                            
		            }
		        }
		    }
		}
		return jcList;
	}    
}
