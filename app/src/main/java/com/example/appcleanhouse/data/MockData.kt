package com.example.appcleanhouse.data

import com.example.appcleanhouse.R
import com.example.appcleanhouse.models.Cleaner
import com.example.appcleanhouse.models.Order
import com.example.appcleanhouse.models.Service

object MockData {
    val MOCK_SERVICES = listOf(
        Service(
            id = "s1",
            name = "Deep Clean",
            description = "Thorough cleaning for every corner including appliances and baseboards.",
            pricePerHour = 45,
            rating = 4.9,
            colorResId = R.drawable.bg_service_blue,
            iconResId = R.drawable.ic_sparkles
        ),
        Service(
            id = "s2",
            name = "Standard",
            description = "Regular maintenance cleaning for keeping your home fresh. Includes comprehensive dusting, sweeping, mopping, and basic tidying of living areas and bedrooms, ensuring a consistently clean environment for you and your family.",
            pricePerHour = 30,
            rating = 4.7,
            colorResId = R.drawable.bg_service_teal,
            iconResId = R.drawable.ic_warehouse
        ),
        Service(
            id = "s3",
            name = "Laundry",
            description = "Professional washing, drying, and folding of clothes and linens. We handle your garments, including delicate fabrics, with the utmost care, ensuring everything is returned fresh, perfectly folded, and ready to wear.",
            pricePerHour = 25,
            rating = 4.8,
            colorResId = R.drawable.bg_service_indigo,
            iconResId = R.drawable.ic_laundry
        ),
        Service(
            id = "s4",
            name = "Carpet",
            description = "Deep steam and extraction cleaning to remove stubborn stains, deep-seated dirt, and allergens. We restore the plush look and soft feel of your carpets using professional-grade, eco-friendly equipment.",
            pricePerHour = 60,
            rating = 4.6,
            colorResId = R.drawable.bg_service_orange,
            iconResId = R.drawable.ic_wind
        )
    )

    val MOCK_CLEANERS = listOf(
        Cleaner(
            id = "c1",
            name = "Phan Khai",
            avatarResId = R.drawable.idol1,
            rating = 4.9,
            jobCount = 142,
            specialty = "Deep Clean Specialist",
            experience = "6 yrs",
            about = "Khai is known for deep-clean precision and kitchen revival. He is punctual, detail-oriented, and consistently gets 5-star feedback from returning clients.",
            pricePerHour = 48,
            tags = listOf("Deep Clean", "Kitchen", "Sanitize"),
            distanceKm = 1.8
        ),
        Cleaner(
            id = "c2",
            name = "Hung Nguyen",
            avatarResId = R.drawable.idol2,
            rating = 4.8,
            jobCount = 98,
            specialty = "Fast & Efficient",
            experience = "4 yrs",
            about = "Hung specializes in fast but meticulous standard cleaning. Great choice for busy weekdays when you need reliable quality in less time.",
            pricePerHour = 42,
            tags = listOf("Standard", "Speed", "Organize"),
            distanceKm = 3.2
        ),
        Cleaner(
            id = "c3",
            name = "Khoa Tran",
            avatarResId = R.drawable.avt3,
            rating = 5.0,
            jobCount = 215,
            specialty = "Pet Friendly",
            experience = "7 yrs",
            about = "Khoa is a premium cleaner trusted by pet owners. He removes fur, odor, and hidden dust effectively while using safe products for animals.",
            pricePerHour = 55,
            tags = listOf("Pet Friendly", "Allergy", "Odor Care"),
            distanceKm = 2.4
        ),
        Cleaner(
            id = "c4",
            name = "Khoi Le",
            avatarResId = R.drawable.avt4,
            rating = 4.7,
            jobCount = 76,
            specialty = "Eco-Friendly Products",
            experience = "3 yrs",
            about = "Khoi focuses on eco-safe products and gentle textile care. Ideal for families that prefer low-chemical and sustainable cleaning routines.",
            pricePerHour = 40,
            tags = listOf("Eco", "Laundry", "Carpet"),
            distanceKm = 4.6
        )
    )

    val MOCK_ORDERS = listOf(
        Order(
            id = "o1",
            serviceId = "s1",
            cleanerId = "c1",
            date = "Oct 24, 2023",
            time = "09:00 AM",
            status = "Upcoming",
            totalPrice = 135.00,
            address = "QL22, Ho Chi Minh"
        ),
        Order(
            id = "o2",
            serviceId = "s2",
            cleanerId = "c2",
            date = "Oct 10, 2023",
            time = "02:00 PM",
            status = "Completed",
            totalPrice = 90.00,
            address = "Le Hong Phong, Ho Chi Minh",
            rating = 5
        ),
        Order(
            id = "o3",
            serviceId = "s3",
            cleanerId = "c3",
            date = "Sep 28, 2023",
            time = "11:00 AM",
            status = "Cancelled",
            totalPrice = 50.00,
            address = "123 Main St, New York"
        ),
        Order(
            id = "o4",
            serviceId = "s2",
            cleanerId = "c2",
            date = "Sep 15, 2023",
            time = "10:00 AM",
            status = "Completed",
            totalPrice = 90.00,
            address = "123 Main St, New York",
            rating = 4
        )
    )
}
