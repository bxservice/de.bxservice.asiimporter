/***********************************************************************
 * This file is part of iDempiere ERP Open Source                      *
 * http://www.idempiere.org                                            *
 *                                                                     *
 * Copyright (C) Contributors                                          *
 *                                                                     *
 * This program is free software; you can redistribute it and/or       *
 * modify it under the terms of the GNU General Public License         *
 * as published by the Free Software Foundation; either version 2      *
 * of the License, or (at your option) any later version.              *
 *                                                                     *
 * This program is distributed in the hope that it will be useful,     *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of      *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the        *
 * GNU General Public License for more details.                        *
 *                                                                     *
 * You should have received a copy of the GNU General Public License   *
 * along with this program; if not, write to the Free Software         *
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,          *
 * MA 02110-1301, USA.                                                 *
 *                                                                     *
 * Contributors:                                                       *
 * - Diego Ruiz - BX Service                                           *
 **********************************************************************/
package de.bxservice.asiimporter.process;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.StringTokenizer;

import org.compiere.model.MAttribute;
import org.compiere.model.MAttributeSet;
import org.compiere.model.MAttributeSetInstance;
import org.compiere.model.MAttributeValue;
import org.compiere.model.MProduct;
import org.compiere.model.Query;
import org.compiere.process.SvrProcess;

public class FillImportProductASI extends SvrProcess {
	
	private final static int LENGTH_POSITION = 0;
	private final static int WIDTH_POSITION  = 1;
	private final static int HEIGHT_POSITION = 2;
	private final static int UOM_POSITION    = 3;
	
	private final static String LENGTH = "Length";
	private final static String WIDTH  = "Width";
	private final static String HEIGHT = "Height";
	private final static String UOM = "Einheit";

	@Override
	protected void prepare() {
	}

	@Override
	protected String doIt() throws Exception {
		int count = 0;
		List <MProduct> products = getProductsWithASIToImport();

		for (MProduct product : products) {
			
			TempASI tempASI = getASIValues(product.getDocumentNote());
			MAttributeSet attributeSet = new MAttributeSet(getCtx(), product.getM_AttributeSet_ID(), get_TrxName());
			MAttributeSetInstance asi = MAttributeSetInstance.create(getCtx(), product, get_TrxName());

			int M_AttributeSetInstance_ID = asi.getM_AttributeSetInstance_ID();
			
			for (MAttribute attribute : attributeSet.getMAttributes(false)) {
				switch(attribute.getName()) {
				case LENGTH:
					createBigDecimalMAttributeInstance(attribute, tempASI.getLength(), M_AttributeSetInstance_ID);
					break;
				case WIDTH:
					createBigDecimalMAttributeInstance(attribute, tempASI.getWidth(), M_AttributeSetInstance_ID);
					break;
				case HEIGHT:
					createBigDecimalMAttributeInstance(attribute, tempASI.getHeight(), M_AttributeSetInstance_ID);
					break;
				case UOM:
					createUOMMAttributeInstance(attribute, tempASI.getUom(), M_AttributeSetInstance_ID);
				}
			}
			asi.setDescription();
			asi.saveEx();
			product.setM_AttributeSetInstance_ID(asi.getM_AttributeSetInstance_ID());
			product.setDocumentNote(null);
			product.saveEx();
			count++;
		}
		
		return count + " ASI generated";
	}
	
	/**
	 * Returns a String array with 
	 * length in position 0
	 * width in position 1
	 * height in position 2
	 * uom in position 3
	 * **/
	private TempASI getASIValues(String asiString) {
		String[] asiValues = new String[4];
	    
		StringTokenizer tokenizer = new StringTokenizer(asiString, ",");
	    int i = 0;
	    while (tokenizer.hasMoreElements()) {
	    	String value = tokenizer.nextToken();
	    	asiValues[i++] = value.substring(value.indexOf(":")+1);
	    }
	    
	    TempASI tempASI = new TempASI();
	    tempASI.setLength(asiValues[LENGTH_POSITION]);
	    tempASI.setWidth(asiValues[WIDTH_POSITION]);
	    tempASI.setHeight(asiValues[HEIGHT_POSITION]);
	    tempASI.setUom(asiValues[UOM_POSITION]);
		
		return tempASI;
	}
	
	private List <MProduct> getProductsWithASIToImport() {
		final String whereClause = MProduct.COLUMNNAME_M_AttributeSet_ID + " IS NOT NULL AND " +
				MProduct.COLUMNNAME_DocumentNote + " IS NOT NULL AND " + 
				MProduct.COLUMNNAME_M_AttributeSetInstance_ID + " = 0";

		return new Query(getCtx(), MProduct.Table_Name, whereClause, get_TrxName())
				.setClient_ID()
				.setOnlyActiveRecords(true)
				.setOrderBy("Value")
				.list();
	}
	
	private void createBigDecimalMAttributeInstance(MAttribute attribute, BigDecimal value, int M_AttributeSetInstance_ID) {
		//setMAttributeInstance doesn't work without decimal point
		if (value != null && value.scale() == 0)
			value = value.setScale(1, RoundingMode.HALF_UP);
		
		attribute.setMAttributeInstance(M_AttributeSetInstance_ID, value);
	}
	
	private void createUOMMAttributeInstance(MAttribute attribute, String uomValue, int M_AttributeSetInstance_ID) {
		for (MAttributeValue attributeValue : attribute.getMAttributeValues()) {
			if (attributeValue.getName().equals(uomValue)) {
				attribute.setMAttributeInstance(M_AttributeSetInstance_ID, attributeValue);
				break;
			}
		}
	}
	
	class TempASI {
		BigDecimal length;
		BigDecimal width;
		BigDecimal height;
		String uom;
		
		public BigDecimal getLength() {
			return length;
		}
		public void setLength(String length) {
			this.length = new BigDecimal(length);
		}
		public BigDecimal getWidth() {
			return width;
		}
		public void setWidth(String width) {
			this.width = new BigDecimal(width);
		}
		public BigDecimal getHeight() {
			return height;
		}
		public void setHeight(String height) {
			this.height = new BigDecimal(height);
		}
		public String getUom() {
			return uom;
		}
		public void setUom(String uom) {
			this.uom = uom;
		}
	}

}
